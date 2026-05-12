import { useState, useEffect, useRef } from 'react'
import './index.css'

/**
 * Frontend para el sistema de pagos idempotente con Kafka.
 *
 * Permite enviar solicitudes de pago al servicio Java,
 * que las publica en Kafka para ser procesadas por el servicio Go.
 * Incluye un panel de logs para visualizar los reintentos y respuestas.
 */
function App() {
  const [clientName, setClientName] = useState('Juan Perez')
  const [description, setDescription] = useState('Compra de laptop y accesorios')
  const [amount, setAmount] = useState(1500.00)
  const [idempotencyKey, setIdempotencyKey] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [logs, setLogs] = useState([])
  const [attemptCount, setAttemptCount] = useState(0)
  const logRef = useRef(null)

  // Generar nueva key al iniciar
  const generateKey = () => {
    setIdempotencyKey(crypto.randomUUID())
    setResult(null)
    setAttemptCount(0)
  }

  useEffect(() => { generateKey() }, [])

  // Auto-scroll del panel de logs
  useEffect(() => {
    if (logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight
    }
  }, [logs])

  // Agregar entrada al log
  const addLog = (type, message) => {
    const time = new Date().toLocaleTimeString()
    setLogs(prev => [...prev, { time, type, message }])
  }

  // Enviar pago al API
  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setAttemptCount(prev => prev + 1)

    addLog('sent', `Enviando pago - Key: ${idempotencyKey.substring(0, 8)}...`)
    addLog('sent', `Cliente: ${clientName}, Monto: $${amount}`)

    try {
      const response = await fetch('http://localhost:8081/api/payments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': idempotencyKey
        },
        body: JSON.stringify({ clientName, description, amount: parseFloat(amount) })
      })

      const data = await response.json()

      if (response.status === 504) {
        addLog('error', `TIMEOUT: El servicio Go no respondio despues de ${data.retriesUsed} intentos`)
      } else if (data.status === 'DUPLICATE') {
        addLog('warning', `DUPLICADO detectado: Key ya fue procesada. No se duplico el cobro.`)
      } else if (data.status === 'PROCESSED') {
        addLog('received', `PROCESADO exitosamente por el servicio Go`)
      }

      addLog('received', `Status: ${data.status} | Reintentos usados: ${data.retriesUsed}`)
      setResult(data)

    } catch (err) {
      addLog('error', `Error de conexion: ${err.message}`)
      setResult({
        idempotencyKey,
        status: 'ERROR',
        message: 'No se pudo conectar al servidor Java en http://localhost:8081',
        processedByGo: false,
        retriesUsed: 0
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app">
      <header>
        <img src="https://www.eia.edu.co/wp-content/uploads/2023/07/Logo-principal-scaled.webp" alt="EIA" />
        <h1>Sistema de Pagos con Kafka</h1>
        <p className="subtitle">Ejemplo de idempotencia con colas de mensajes (Java + Go + Kafka)</p>
      </header>

      {/* Formulario de pago */}
      <div className="card">
        <h2>Solicitud de Pago</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Idempotency-Key (UUID)</label>
            <div className="key-box">{idempotencyKey}</div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Nombre del Cliente</label>
              <input type="text" value={clientName} onChange={e => setClientName(e.target.value)} required />
            </div>
            <div className="form-group">
              <label>Monto ($)</label>
              <input type="number" step="0.01" value={amount} onChange={e => setAmount(e.target.value)} required />
            </div>
          </div>

          <div className="form-group">
            <label>Descripcion del Pago</label>
            <input type="text" value={description} onChange={e => setDescription(e.target.value)} required />
          </div>

          <div className="actions">
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Procesando...' : 'Enviar Pago'}
            </button>
            <button type="button" className="btn btn-secondary" onClick={generateKey}>
              Nueva Key
            </button>
          </div>
        </form>

        {/* Resultado */}
        {result && (
          <div className={`result ${result.status}`}>
            <h3>
              {result.status === 'PROCESSED' && 'Pago Procesado Correctamente'}
              {result.status === 'DUPLICATE' && 'Idempotencia Activada - Duplicado Detectado'}
              {result.status === 'TIMEOUT' && 'Timeout - Sin Respuesta del Servicio Go'}
              {result.status === 'ERROR' && 'Error de Conexion'}
            </h3>
            <p>{result.message}</p>
            <p>Reintentos usados: <strong>{result.retriesUsed}</strong> | Intentos con esta key: <strong>{attemptCount}</strong></p>
            {result.status === 'DUPLICATE' && (
              <p><strong>Nota:</strong> Aunque enviaste el pago de nuevo, el servicio Go reconocio la clave y NO proceso el cobro dos veces.</p>
            )}
          </div>
        )}
      </div>

      {/* Panel de logs */}
      <div className="card">
        <h2>Log de Actividad</h2>
        <div className="log-panel" ref={logRef}>
          {logs.length === 0 && (
            <div className="log-entry" style={{color: '#888'}}>
              Esperando solicitudes...
            </div>
          )}
          {logs.map((entry, i) => (
            <div key={i} className={`log-entry ${entry.type}`}>
              <span className="time">[{entry.time}]</span> {entry.message}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default App
