import { useState, useEffect, useRef } from 'react'
import './index.css'

// URLs de los microservicios (accesibles desde el browser via puertos mapeados en Docker)
const ORDER_API = 'http://localhost:8081/api/orders'
const PAYMENT_API = 'http://localhost:8082/api/payments'

function App() {
  // Formulario
  const [customerName, setCustomerName] = useState('Juan Perez')
  const [customerEmail, setCustomerEmail] = useState('juan@eia.edu.co')
  const [productName, setProductName] = useState('Laptop Dell XPS')
  const [amount, setAmount] = useState(2500.00)
  const [currency, setCurrency] = useState('USD')

  // Estado
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [cbStatus, setCbStatus] = useState(null)
  const [paymentHealth, setPaymentHealth] = useState(null)
  const [orders, setOrders] = useState([])
  const [logs, setLogs] = useState([])
  const logRef = useRef(null)

  useEffect(() => {
    if (logRef.current) logRef.current.scrollTop = logRef.current.scrollHeight
  }, [logs])

  // Polling de estado cada 2s
  useEffect(() => {
    const poll = () => { fetchCbStatus(); fetchPaymentHealth(); fetchOrders() }
    poll()
    const interval = setInterval(poll, 2000)
    return () => clearInterval(interval)
  }, [])

  const addLog = (type, msg) => {
    setLogs(prev => [...prev, { time: new Date().toLocaleTimeString(), type, msg }])
  }

  const fetchCbStatus = async () => {
    try {
      const res = await fetch(`${ORDER_API}/cb/status`)
      setCbStatus(await res.json())
    } catch { /* backend no disponible */ }
  }

  const fetchPaymentHealth = async () => {
    try {
      const res = await fetch(`${PAYMENT_API}/health`)
      setPaymentHealth(await res.json())
    } catch { setPaymentHealth(null) }
  }

  const fetchOrders = async () => {
    try {
      const res = await fetch(ORDER_API)
      setOrders(await res.json())
    } catch { /* */ }
  }

  // Crear orden
  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    addLog('request', `Creando orden - ${customerName} - ${productName} - $${amount} ${currency}`)

    try {
      const res = await fetch(ORDER_API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ customerName, customerEmail, productName, amount: parseFloat(amount), currency })
      })
      const data = await res.json()

      if (data.status === 'PAID') {
        addLog('success', `PAGADO - Orden ${data.orderId} - PaymentID: ${data.paymentId}`)
      } else if (data.status === 'PAYMENT_PENDING') {
        addLog('fallback', `FALLBACK - Orden ${data.orderId} - ${data.statusMessage}`)
      }
      setResult(data)
      fetchCbStatus()
      fetchOrders()
    } catch (err) {
      addLog('error', `Error de conexion: ${err.message}`)
      setResult({ status: 'ERROR', statusMessage: 'No se pudo conectar al order-service' })
    } finally {
      setLoading(false)
    }
  }

  // Rafaga
  const sendBurst = async (count) => {
    addLog('info', `Iniciando rafaga de ${count} ordenes...`)
    for (let i = 0; i < count; i++) {
      await handleBurstOrder(i + 1)
      await new Promise(r => setTimeout(r, 300))
    }
    addLog('info', `Rafaga completada`)
    fetchCbStatus()
    fetchOrders()
  }

  const handleBurstOrder = async (n) => {
    try {
      const res = await fetch(ORDER_API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          customerName: `Cliente Rafaga #${n}`,
          customerEmail: `burst${n}@eia.edu.co`,
          productName: `Producto #${n}`,
          amount: 100 + Math.random() * 900,
          currency: 'USD'
        })
      })
      const data = await res.json()
      if (data.status === 'PAID') addLog('success', `#${n} PAGADO - ${data.orderId}`)
      else addLog('fallback', `#${n} FALLBACK - ${data.orderId}`)
    } catch (err) {
      addLog('error', `#${n} Error: ${err.message}`)
    }
  }

  // Toggle payment service
  const togglePayment = async () => {
    try {
      const res = await fetch(`${PAYMENT_API}/toggle`, { method: 'POST' })
      const data = await res.json()
      addLog('status', `Payment Service: ${data.available ? 'ACTIVADO' : 'DESACTIVADO'}`)
      fetchPaymentHealth()
    } catch (err) {
      addLog('error', `Error al toggle: ${err.message}`)
    }
  }

  // Reset CB
  const resetCB = async () => {
    try {
      await fetch(`${ORDER_API}/cb/reset`, { method: 'POST' })
      addLog('status', 'Circuit Breaker REINICIADO')
      fetchCbStatus()
    } catch (err) { addLog('error', `Error: ${err.message}`) }
  }

  const stateEmoji = { CLOSED: '🟢', OPEN: '🔴', HALF_OPEN: '🟡' }
  const stateText = { CLOSED: 'Cerrado (Normal)', OPEN: 'Abierto (Protegiendo)', HALF_OPEN: 'Semi-Abierto (Probando)' }

  return (
    <div className="app">
      <header>
        <img src="https://www.eia.edu.co/wp-content/uploads/2023/07/Logo-principal-scaled.webp" alt="Universidad EIA" />
        <h1>Circuit Breaker - Microservicios</h1>
        <p className="subtitle">Order Service → Payment Service | Spring Cloud + Eureka + Resilience4j + H2</p>
      </header>

      <div className="main-grid">
        {/* Panel izquierdo: Status */}
        <div>
          {/* Circuit Breaker Status */}
          <div className="card">
            <h2>Circuit Breaker (Order Service)</h2>
            {cbStatus ? (<>
              <div className={`status-panel ${cbStatus.state}`}>
                <div className={`status-indicator ${cbStatus.state}`}>{stateEmoji[cbStatus.state] || '⚪'}</div>
                <div className="status-info">
                  <h3>Circuit Breaker</h3>
                  <span className={`state-label ${cbStatus.state}`}>{stateText[cbStatus.state] || cbStatus.state}</span>
                </div>
              </div>
              <div className="metrics-grid">
                <div className="metric-card"><span className="metric-value">{cbStatus.bufferedCalls}</span><span className="metric-label">Total</span></div>
                <div className="metric-card"><span className="metric-value">{cbStatus.successfulCalls}</span><span className="metric-label">Exitosas</span></div>
                <div className="metric-card"><span className="metric-value">{cbStatus.failedCalls}</span><span className="metric-label">Fallidas</span></div>
                <div className="metric-card"><span className="metric-value">{cbStatus.failureRate}%</span><span className="metric-label">Tasa Fallos</span></div>
                <div className="metric-card"><span className="metric-value">{cbStatus.notPermittedCalls}</span><span className="metric-label">Rechazadas</span></div>
                <div className="metric-card"><span className="metric-value">{orders.length}</span><span className="metric-label">Ordenes</span></div>
              </div>
              <div className="actions" style={{marginTop:'1rem'}}><button className="btn btn-danger btn-sm" onClick={resetCB}>Reiniciar CB</button></div>
            </>) : <p className="muted">Conectando al order-service...</p>}
          </div>

          {/* Payment Service Health */}
          <div className="card">
            <h2>Payment Service</h2>
            <div className={`status-panel ${paymentHealth?.available ? 'CLOSED' : 'OPEN'}`}>
              <div className={`status-indicator ${paymentHealth?.available ? 'CLOSED' : 'OPEN'}`}>
                {paymentHealth?.available ? '✓' : '✕'}
              </div>
              <div className="status-info">
                <h3>Servicio de Pagos</h3>
                <span className={`state-label ${paymentHealth?.available ? 'CLOSED' : 'OPEN'}`}>
                  {paymentHealth ? (paymentHealth.available ? 'Activo - Procesando pagos' : 'Desactivado - Rechazando todo') : 'Sin conexion'}
                </span>
              </div>
            </div>
            <div className="actions">
              <button className={`btn ${paymentHealth?.available ? 'btn-danger' : 'btn-primary'} btn-sm`} onClick={togglePayment} style={{flex:1}}>
                {paymentHealth?.available ? 'Desactivar Payment Service' : 'Activar Payment Service'}
              </button>
            </div>
          </div>
        </div>

        {/* Panel derecho: Formulario */}
        <div className="card">
          <h2>Crear Orden</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-row">
              <div className="form-group"><label>Cliente</label><input type="text" value={customerName} onChange={e => setCustomerName(e.target.value)} required /></div>
              <div className="form-group"><label>Email</label><input type="email" value={customerEmail} onChange={e => setCustomerEmail(e.target.value)} required /></div>
            </div>
            <div className="form-group"><label>Producto</label><input type="text" value={productName} onChange={e => setProductName(e.target.value)} required /></div>
            <div className="form-row">
              <div className="form-group"><label>Monto</label><input type="number" step="0.01" value={amount} onChange={e => setAmount(e.target.value)} required /></div>
              <div className="form-group"><label>Moneda</label>
                <select value={currency} onChange={e => setCurrency(e.target.value)}>
                  <option value="USD">USD</option><option value="COP">COP</option><option value="EUR">EUR</option>
                </select>
              </div>
            </div>
            <div className="actions">
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? <><span className="spinner"></span> Procesando...</> : 'Crear Orden'}
              </button>
            </div>
          </form>

          <div className="rapid-fire">
            <span className="rapid-fire-label">Rafaga (para probar Circuit Breaker):</span>
            <button className="btn btn-secondary btn-sm" onClick={() => sendBurst(5)} disabled={loading}>5 ordenes</button>
            <button className="btn btn-secondary btn-sm" onClick={() => sendBurst(10)} disabled={loading}>10 ordenes</button>
            <button className="btn btn-secondary btn-sm" onClick={() => sendBurst(15)} disabled={loading}>15 ordenes</button>
          </div>

          {result && (
            <div className={`result ${result.status}`}>
              <h3>{result.status === 'PAID' ? 'Orden Pagada' : result.status === 'PAYMENT_PENDING' ? 'Pago Pendiente (Fallback)' : 'Error'}</h3>
              <p>{result.statusMessage}</p>
              {result.paymentId && <p><strong>Payment ID:</strong> {result.paymentId}</p>}
            </div>
          )}
        </div>
      </div>

      {/* Tabla de Ordenes */}
      {orders.length > 0 && (
        <div className="card">
          <h2>Ordenes en Base de Datos (H2)</h2>
          <div style={{overflowX:'auto'}}>
            <table className="orders-table">
              <thead><tr><th>Orden</th><th>Cliente</th><th>Producto</th><th>Monto</th><th>Estado</th><th>Payment ID</th></tr></thead>
              <tbody>
                {orders.slice().reverse().slice(0, 15).map((o, i) => (
                  <tr key={i} className={o.status}>
                    <td>{o.orderId}</td><td>{o.customerName}</td><td>{o.productName}</td>
                    <td>${o.amount.toFixed(2)} {o.currency}</td>
                    <td><span className={`badge ${o.status}`}>{o.status}</span></td>
                    <td>{o.paymentId || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Logs */}
      <div className="card">
        <div className="log-header">
          <h2>Log de Actividad</h2>
          <button className="clear-logs" onClick={() => setLogs([])}>Limpiar</button>
        </div>
        <div className="log-panel" ref={logRef}>
          {logs.length === 0 && <div className="log-entry" style={{color:'#475569'}}>Esperando solicitudes...</div>}
          {logs.map((e, i) => <div key={i} className={`log-entry ${e.type}`}><span className="time">[{e.time}]</span> {e.msg}</div>)}
        </div>
      </div>
    </div>
  )
}

export default App
