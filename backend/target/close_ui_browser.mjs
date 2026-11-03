try {
  const info = await (await fetch('http://localhost:19222/json/version')).json()
  const socket = new WebSocket(info.webSocketDebuggerUrl)
  await new Promise((resolve, reject) => { socket.addEventListener('open', resolve, { once: true }); socket.addEventListener('error', reject, { once: true }) })
  socket.send(JSON.stringify({ id: 1, method: 'Browser.close' }))
  await new Promise(resolve => { socket.addEventListener('close', resolve, { once: true }); setTimeout(resolve, 3000) })
} catch (_) { /* Le navigateur peut déjà être fermé. */ }
