import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)
const { parse, compileScript, compileTemplate } = require('../../frontend/node_modules/@vue/compiler-sfc')
for (const file of ['components/assignation/ChoixDossier.vue', 'views/MarchandiseListView.vue', 'views/ConteneurListView.vue']) {
  const source = readFileSync(new URL('../../frontend/src/' + file, import.meta.url), 'utf8')
  const { descriptor, errors } = parse(source, { filename: file })
  if (errors.length) throw errors[0]
  const script = compileScript(descriptor, { id: file })
  const result = compileTemplate({ source: descriptor.template.content, filename: file, id: file,
    compilerOptions: { bindingMetadata: script.bindings } })
  if (result.errors.length) throw result.errors[0]
  console.log('Compilation Vue OK : ' + file)
}
