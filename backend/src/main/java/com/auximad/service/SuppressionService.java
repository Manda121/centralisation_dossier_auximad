package com.auximad.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/** Retire les dépendances selon les contraintes effectives, sans effacer les objets partagés. */
@Service
public class SuppressionService {
    public static final String CONFIRMATION_HEADER = "X-Confirmation-Suppression";
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("ordre_transit", "ordres de transit"), Map.entry("dossier", "dossiers"),
            Map.entry("contrat_transport", "contrats de transport"), Map.entry("instruction", "instructions"),
            Map.entry("marchandise", "marchandises"), Map.entry("document", "documents"),
            Map.entry("remise", "remises de documents"), Map.entry("document_mvt", "liens document-mouvement"),
            Map.entry("dossier_conteneur", "assignations de conteneurs"), Map.entry("dossier_interagence", "liens interagences"),
            Map.entry("demande_interagence", "demandes interagences"), Map.entry("mvt_dossier", "mouvements"),
            Map.entry("operation_dossier", "opérations de dossier"), Map.entry("declaration", "déclarations"), Map.entry("expedition", "expéditions"),
            Map.entry("constat", "constats"), Map.entry("groupage", "groupages"),
            Map.entry("clients", "clients"), Map.entry("conteneur", "conteneurs"),
            Map.entry("utilisateur", "utilisateurs"), Map.entry("role", "rôles"),
            Map.entry("autorisation", "autorisations"), Map.entry("suspension_connexion", "suspensions de connexion"),
            Map.entry("historique", "historiques"), Map.entry("action", "actions"), Map.entry("agence", "agences"),
            Map.entry("bureau", "bureaux"), Map.entry("mode_transport", "modes de transport"),
            Map.entry("type_production", "types de production"), Map.entry("type_regime", "types de régime"),
            Map.entry("phase_dossier", "phases de dossier"), Map.entry("statut_dossier", "statuts de dossier"),
            Map.entry("statut_ot", "statuts OT"), Map.entry("inconterme", "incoterms"),
            Map.entry("nature_marchandise", "natures de marchandise"), Map.entry("unite_mesure", "unités de mesure"),
            Map.entry("type_container", "types de conteneur"), Map.entry("type_document", "types de document"),
            Map.entry("compagnie", "compagnies"), Map.entry("navire", "navires"), Map.entry("pays", "pays"));
    private static final Set<String> ROOTS = LABELS.keySet();
    private static final Set<String> ASSOCIATIONS = Set.of("dossier_conteneur", "document_mvt", "dossier_interagence");
    private final JdbcTemplate jdbc;
    @PersistenceContext private EntityManager entityManager;

    public SuppressionService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Detail(String objet, long nombre) {}
    public record Impact(String confirmation, List<Detail> suppressions, List<Detail> liensRetires,
                         long dossiersClotures) {}
    private record Key(String table, SortedMap<String, Object> values) {
        String canonical() { return table + values; }
    }
    private record Relation(String child, String column, String parent, String parentColumn, boolean required) {}
    private record Unlink(Key row, String column, boolean closeDossier) {}
    private static final class Plan {
        final LinkedHashSet<Key> deletes = new LinkedHashSet<>();
        final LinkedHashSet<Unlink> unlinks = new LinkedHashSet<>();
        final Set<Key> visiting = new HashSet<>();
    }
    private record Schema(String name, Map<String, List<String>> primaryKeys, List<Relation> relations) {}

    @Transactional
    public void supprimer(String table, Integer id) {
        if (id == null) throw new IllegalArgumentException("Identifiant de suppression manquant.");
        supprimerLot(table, List.of(id));
    }

    @Transactional
    public void supprimerLot(String table, List<Integer> ids) {
        if (!ROOTS.contains(table) || ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Suppression invalide.");
        }
        Schema schema = schema();
        String confirmation = confirmation();
        Plan plan = new Plan();
        for (Integer id : new TreeSet<>(ids)) {
            Key root = new Key(table, new TreeMap<>(Map.of("id", id)));
            if (selectKeys(schema, table, "id", id, confirmation != null).isEmpty()) {
                throw new IllegalArgumentException("Objet introuvable : " + label(table) + " #" + id);
            }
            visit(schema, root, plan, confirmation != null);
        }
        executeConfirmed(schema, plan, confirmation);
    }

    /** Retire uniquement une assignation : les autres dossiers conservent leur conteneur. */
    @Transactional
    public void retirerAssignationConteneur(Integer conteneurId, Integer dossierId) {
        Schema schema = schema();
        String confirmation = confirmation();
        if (selectKeys(schema, "conteneur", "id", conteneurId, confirmation != null).isEmpty()) {
            throw new IllegalArgumentException("Conteneur introuvable.");
        }
        List<Key> assignments = selectKeys(schema, "dossier_conteneur", "id_conteneur", conteneurId, confirmation != null);
        Key target = assignments.stream().filter(k -> Objects.equals(k.values().get("id_dossier"), dossierId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Assignation introuvable."));
        Plan plan = new Plan();
        if (assignments.size() == 1) {
            visit(schema, new Key("conteneur", new TreeMap<>(Map.of("id", conteneurId))), plan, confirmation != null);
        } else {
            visit(schema, target, plan, confirmation != null);
        }
        executeConfirmed(schema, plan, confirmation);
    }

    private void visit(Schema schema, Key key, Plan plan, boolean lock) {
        if (plan.deletes.contains(key)) return;
        if (!plan.visiting.add(key)) {
            throw new IllegalArgumentException("Dépendances cycliques : suppression annulée.");
        }
        for (Relation relation : schema.relations()) {
            if (!relation.parent().equals(key.table())) continue;
            Object parentValue = key.values().get(relation.parentColumn());
            if (parentValue == null) throw new IllegalArgumentException("Clé étrangère non prise en charge.");
            for (Key child : selectKeys(schema, relation.child(), relation.column(), parentValue, lock)) {
                boolean close = relation.parent().equals("ordre_transit") && relation.child().equals("dossier")
                        && relation.column().equals("id_ordre_transit");
                if (close && relation.required()) {
                    throw new IllegalArgumentException("Le lien OT du dossier est obligatoire dans cette base. Suppression annulée pour conserver le dossier.");
                }
                if (owned(relation) || relation.required()) {
                    visit(schema, child, plan, lock);
                } else {

                    plan.unlinks.add(new Unlink(child, relation.column(), close));
                }
            }
        }
        plan.visiting.remove(key);
        plan.deletes.add(key); // Enfants avant parents, dédoublonnés pour les chemins multiples.
    }

    private boolean owned(Relation relation) {
        if (ASSOCIATIONS.contains(relation.child())) return true;
        if (relation.parent().equals("dossier") && relation.column().equals("id_dossier")) {
            return !relation.child().equals("conteneur"); // Ancienne colonne : conteneur partagé, à détacher.
        }
        if (relation.parent().equals("ordre_transit")) {
            return Set.of("contrat_transport", "instruction", "marchandise", "expedition").contains(relation.child());
        }
        if (relation.parent().equals("utilisateur")) {
            return Set.of("autorisation", "suspension_connexion").contains(relation.child());
        }
        return relation.parent().equals("expedition") && relation.child().equals("groupage");
    }

    private void executeConfirmed(Schema schema, Plan plan, String confirmation) {
        plan.unlinks.removeIf(unlink -> plan.deletes.contains(unlink.row()));
        Impact impact = impact(schema, plan);
        if (!impact.confirmation().equals(confirmation)) throw new SuppressionConfirmationException(impact);
        Integer closedStatus = impact.dossiersClotures() > 0 ? closedStatus(schema) : null;
        for (Unlink unlink : plan.unlinks) {
            String changes = quote(unlink.column()) + " = NULL";
            List<Object> args = new ArrayList<>();
            if (unlink.closeDossier()) { changes += ", id_statut = ?"; args.add(closedStatus); }
            args.addAll(unlink.row().values().values());
            jdbc.update("UPDATE " + qualified(schema, unlink.row().table()) + " SET " + changes
                    + " WHERE " + predicate(unlink.row()), args.toArray());
        }
        for (Key key : plan.deletes) {
            jdbc.update("DELETE FROM " + qualified(schema, key.table()) + " WHERE " + predicate(key), key.values().values().toArray());
        }
        if (entityManager != null) entityManager.clear();
    }

    private Impact impact(Schema schema, Plan plan) {
        Map<String, Long> deleted = plan.deletes.stream().collect(Collectors.groupingBy(k -> label(k.table()), TreeMap::new, Collectors.counting()));
        Map<String, Set<Key>> detached = new TreeMap<>();
        Set<Key> closed = new HashSet<>();
        List<String> actions = new ArrayList<>();
        actions.add("schema:" + schema.name());
        for (Key key : plan.deletes) actions.add("delete:" + key.canonical());
        for (Unlink unlink : plan.unlinks) {
            detached.computeIfAbsent(label(unlink.row().table()), ignored -> new HashSet<>()).add(unlink.row());
            if (unlink.closeDossier()) closed.add(unlink.row());
            actions.add("unlink:" + unlink.row().canonical() + ":" + unlink.column() + ":" + unlink.closeDossier());
        }
        Collections.sort(actions);
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(String.join("\n", actions).getBytes(StandardCharsets.UTF_8)));
            return new Impact(digest, deleted.entrySet().stream().map(e -> new Detail(e.getKey(), e.getValue())).toList(),
                    detached.entrySet().stream().map(e -> new Detail(e.getKey(), e.getValue().size())).toList(), closed.size());
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private Integer closedStatus(Schema schema) {
        List<Integer> ids = jdbc.queryForList("SELECT id FROM " + qualified(schema, "statut_dossier")
                + " WHERE translate(lower(nom), 'ôéèêà', 'oeeea') LIKE '%clotur%' ORDER BY id LIMIT 1", Integer.class);
        if (!ids.isEmpty()) return ids.get(0);
        // Évite deux créations concurrentes du statut lorsqu'il n'existe pas encore.
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtext(?), hashtext(?))", schema.name(), "statut_dossier_cloture");
        ids = jdbc.queryForList("SELECT id FROM " + qualified(schema, "statut_dossier")
                + " WHERE translate(lower(nom), 'ôéèêà', 'oeeea') LIKE '%clotur%' ORDER BY id LIMIT 1", Integer.class);
        if (!ids.isEmpty()) return ids.get(0);
        return jdbc.queryForObject("INSERT INTO " + qualified(schema, "statut_dossier")
                + " (nom, description) VALUES ('Clôturé', 'Dossier clôturé') RETURNING id", Integer.class);
    }

    private Schema schema() {
        String name = jdbc.queryForObject("SELECT current_schema()", String.class);
        Map<String, List<String>> primaryKeys = new HashMap<>();
        jdbc.queryForList("""
                SELECT t.relname AS table_name, a.attname AS column_name
                FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid
                JOIN pg_namespace n ON n.oid=t.relnamespace
                CROSS JOIN LATERAL unnest(c.conkey) WITH ORDINALITY AS k(num, pos)
                JOIN pg_attribute a ON a.attrelid=t.oid AND a.attnum=k.num
                WHERE c.contype='p' AND n.nspname=? ORDER BY t.relname,k.pos
                """, name).forEach(row -> primaryKeys.computeIfAbsent((String) row.get("table_name"), ignored -> new ArrayList<>())
                        .add((String) row.get("column_name")));
        List<Relation> relations = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList("""
                SELECT child.relname AS child, parent.relname AS parent, ca.attname AS column_name,
                       pa.attname AS parent_column, ca.attnotnull AS required,
                       cardinality(c.conkey) AS key_size, pn.nspname AS parent_schema
                FROM pg_constraint c JOIN pg_class child ON child.oid=c.conrelid
                JOIN pg_class parent ON parent.oid=c.confrelid
                JOIN pg_namespace cn ON cn.oid=child.relnamespace
                JOIN pg_namespace pn ON pn.oid=parent.relnamespace
                JOIN pg_attribute ca ON ca.attrelid=child.oid AND ca.attnum=c.conkey[1]
                JOIN pg_attribute pa ON pa.attrelid=parent.oid AND pa.attnum=c.confkey[1]
                WHERE c.contype='f' AND (cn.nspname=? OR pn.nspname=?)
                ORDER BY parent.relname,child.relname,ca.attname
                """, name, name)) {
            if (((Number) row.get("key_size")).intValue() != 1 || !name.equals(row.get("parent_schema"))
                    || !primaryKeys.containsKey((String) row.get("child"))) {
                throw new IllegalArgumentException("Contrainte complexe détectée : suppression annulée sans modification.");
            }
            relations.add(new Relation((String) row.get("child"), (String) row.get("column_name"),
                    (String) row.get("parent"), (String) row.get("parent_column"), (Boolean) row.get("required")));
        }
        return new Schema(name, primaryKeys, relations);
    }

    private List<Key> selectKeys(Schema schema, String table, String column, Object value, boolean lock) {
        List<String> pk = schema.primaryKeys().get(table);
        if (pk == null || pk.isEmpty()) throw new IllegalArgumentException("Clé primaire introuvable : " + table);
        String fields = pk.stream().map(SuppressionService::quote).collect(Collectors.joining(", "));
        return jdbc.queryForList("SELECT " + fields + " FROM " + qualified(schema, table)
                        + " WHERE " + quote(column) + " = ? ORDER BY " + fields + (lock ? " FOR UPDATE" : ""), value).stream()
                .map(row -> new Key(table, new TreeMap<>(row))).toList();
    }

    private String confirmation() {
        var attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servlet
                ? servlet.getRequest().getHeader(CONFIRMATION_HEADER) : null;
    }
    private static String label(String table) { return LABELS.getOrDefault(table, table.replace('_', ' ')); }
    private static String quote(String identifier) { return "\"" + identifier.replace("\"", "\"\"") + "\""; }
    private static String qualified(Schema schema, String table) { return quote(schema.name()) + "." + quote(table); }
    private static String predicate(Key key) { return key.values().keySet().stream().map(k -> quote(k) + " = ?").collect(Collectors.joining(" AND ")); }
}