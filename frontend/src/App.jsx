import { useState, useEffect } from 'react'
import './index.css'

/**
 * Componente principal del sistema de compras idempotente.
 * 
 * Demuestra cómo un frontend puede:
 * 1. Generar un UUID único (Idempotency-Key) para cada transacción
 * 2. Enviar múltiples veces la misma solicitud con la misma Key
 * 3. Verificar que el servidor solo procesa UNA vez la compra
 * 
 * El botón "Realizar Compra" se puede presionar múltiples veces.
 * Solo la primera vez crea un registro en la base de datos.
 * Las siguientes veces, el servidor devuelve el resultado original.
 */
function App() {
  // Estado del formulario
  const [clientName, setClientName] = useState('Juan Pérez')
  const [productDetails, setProductDetails] = useState('Laptop, Ratón, Teclado')
  const [totalAmount, setTotalAmount] = useState(1500.50)
  
  // Estado de la idempotencia
  const [idempotencyKey, setIdempotencyKey] = useState('')
  const [status, setStatus] = useState(null)
  const [loading, setLoading] = useState(false)
  const [orderCount, setOrderCount] = useState(0)

  // Genera un UUID nuevo al cargar la página
  const generateNewKey = () => {
    setIdempotencyKey(crypto.randomUUID())
    setStatus(null)
    setOrderCount(0)
  }

  useEffect(() => {
    generateNewKey()
  }, [])

  // Enviar la solicitud de compra al API
  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    
    try {
      const response = await fetch('http://localhost:8080/api/purchases', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': idempotencyKey
        },
        body: JSON.stringify({
          clientName,
          productDetails,
          totalAmount: parseFloat(totalAmount)
        })
      })

      const data = await response.json()
      setOrderCount(prev => prev + 1)

      if (response.status === 201) {
        // Primera vez: la compra fue creada exitosamente
        setStatus({ 
          type: 'success', 
          text: `✅ ¡Compra creada exitosamente! Orden ID: ${data.id}` 
        })
      } else if (response.status === 200 && data.alreadyProcessed) {
        // Idempotencia activada: la compra ya existía
        setStatus({ 
          type: 'warning', 
          text: `⚠️ Idempotencia activada. La compra ya había sido procesada. Se devolvió la Orden ID: ${data.id} sin duplicar.` 
        })
      }
    } catch (err) {
      setStatus({ 
        type: 'error', 
        text: '❌ Error de red. Asegúrate de que el backend esté corriendo en http://localhost:8080' 
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app-container">
      <header>
        <img src="https://www.eia.edu.co/wp-content/uploads/2023/07/Logo-principal-scaled.webp" alt="Logo EIA" />
        <h1>Sistema de Compras Idempotente</h1>
        <p style={{color: '#666', fontSize: '0.95rem', marginTop: '-0.5rem'}}>
          Presiona "Realizar Compra" múltiples veces con la misma Key para ver la idempotencia en acción.
        </p>
      </header>

      <div className="card">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>🔑 Idempotency-Key (UUID único por transacción)</label>
            <div className="key-display">{idempotencyKey}</div>
          </div>
          
          <div className="form-group">
            <label>👤 Nombre del Cliente</label>
            <input 
              type="text" 
              value={clientName} 
              onChange={e => setClientName(e.target.value)} 
              required 
            />
          </div>

          <div className="form-group">
            <label>📦 Detalles de los Productos</label>
            <input 
              type="text" 
              value={productDetails} 
              onChange={e => setProductDetails(e.target.value)} 
              required 
            />
          </div>

          <div className="form-group">
            <label>💰 Monto Total ($)</label>
            <input 
              type="number" 
              step="0.01" 
              value={totalAmount} 
              onChange={e => setTotalAmount(e.target.value)} 
              required 
            />
          </div>

          <button type="submit" className="btn" disabled={loading}>
            {loading ? '⏳ Procesando...' : '🛒 Realizar Compra'}
          </button>
          
          <div style={{marginTop: '1rem', textAlign: 'center'}}>
            <button 
              type="button" 
              onClick={generateNewKey} 
              className="btn" 
              style={{backgroundColor: '#C09B42', width: 'auto', padding: '0.7rem 1.5rem'}}
            >
              🔄 Nueva Transacción (Generar nueva Key)
            </button>
          </div>
        </form>

        {status && (
          <div className={`message ${status.type}`}>
            <p>{status.text}</p>
            <p><small>Intentos con esta Key: <strong>{orderCount}</strong></small></p>
            {status.type === 'warning' && (
              <p><small>
                💡 <strong>Explicación:</strong> Aunque hiciste clic de nuevo, el servidor reconoció 
                el <code>Idempotency-Key</code> y NO duplicó el cobro. Devolvió el resultado 
                de la primera solicitud. ¡Eso es idempotencia!
              </small></p>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

export default App
