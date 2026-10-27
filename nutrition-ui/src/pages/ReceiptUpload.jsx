import { useCallback, useState } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { extractReceipt, confirmReceipt } from '../api/nutritionApi'
import styles from './ReceiptUpload.module.css'

const CATEGORIES = [
  '', 'Proteins (Meat / Fish / Poultry)', 'Dairy', 'Produce – Vegetables',
  'Produce – Fruits', 'Grains & Bread', 'Legumes & Beans', 'Nuts & Seeds',
  'Oils & Fats', 'Snacks & Sweets', 'Beverages',
]

export default function ReceiptUpload() {
  const { weekId } = useParams()
  const navigate = useNavigate()

  const [dragging, setDragging] = useState(false)
  const [extracting, setExtracting] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  // Extraction results
  const [extracted, setExtracted] = useState(null) // ExtractedGroceryReceipt
  const [items, setItems] = useState([])           // editable rows
  const [total, setTotal] = useState(null)

  // ── File handling ──────────────────────────────────────────────────────────

  const handleFile = useCallback(async (file) => {
    if (!file) return
    setError(null)
    setExtracting(true)
    try {
      const result = await extractReceipt(file)
      setExtracted(result)
      setTotal(result.total ?? null)
      // Map extracted items to editable rows; pre-fill price from line_total
      setItems((result.items ?? []).map(item => ({
        name: item.name,
        price: item.lineTotal ?? item.unitPrice ?? null,
        category: item.category ?? '',
        servingSizeG: 100,
        servingsPerWeek: 1,
      })))
    } catch (e) {
      setError(e.message)
    } finally {
      setExtracting(false)
    }
  }, [])

  const onDrop = useCallback((e) => {
    e.preventDefault()
    setDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) handleFile(file)
  }, [handleFile])

  const onInputChange = (e) => handleFile(e.target.files[0])

  // ── Row editing ────────────────────────────────────────────────────────────

  function updateItem(i, field, value) {
    setItems(prev => prev.map((row, idx) => idx === i ? { ...row, [field]: value } : row))
  }

  function addRow() {
    setItems(prev => [...prev, { name: '', price: null, category: '', servingSizeG: 100, servingsPerWeek: 1 }])
  }

  function removeRow(i) {
    setItems(prev => prev.filter((_, idx) => idx !== i))
  }

  // ── Confirm ────────────────────────────────────────────────────────────────

  async function handleConfirm() {
    const valid = items.filter(r => r.name.trim() && r.category)
    if (valid.length === 0) {
      setError('Add at least one item with a name and category.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      const payload = valid.map(r => ({
        name: r.name.trim(),
        category: r.category,
        servingSizeG: Number(r.servingSizeG) || 100,
        servingsPerWeek: Number(r.servingsPerWeek) || 1,
        price: r.price != null ? Number(r.price) : null,
      }))
      await confirmReceipt(weekId, payload, total)
      navigate(`/report/${weekId}`)
    } catch (e) {
      setError(e.message)
      setSaving(false)
    }
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <div className={styles.container}>
      <div className={styles.topBar}>
        <Link to={`/report/${weekId}`} className={styles.back}>← Back to report</Link>
        <h1 className={styles.title}>Upload Receipt</h1>
      </div>

      {!extracted && (
        <div
          className={`${styles.dropZone} ${dragging ? styles.dragging : ''}`}
          onDragOver={(e) => { e.preventDefault(); setDragging(true) }}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
        >
          {extracting ? (
            <p className={styles.hint}>Scanning receipt with Claude AI…</p>
          ) : (
            <>
              <p className={styles.dropText}>Drop a receipt photo here</p>
              <p className={styles.dropSub}>or</p>
              <label className={styles.btnUpload}>
                Choose File
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  style={{ display: 'none' }}
                  onChange={onInputChange}
                />
              </label>
              <p className={styles.dropHint}>JPEG, PNG, or WebP · up to 20 MB</p>
            </>
          )}
        </div>
      )}

      {error && <p className={styles.error}>{error}</p>}

      {extracted && (
        <>
          <div className={styles.extractedMeta}>
            {extracted.storeName && <span className={styles.store}>{extracted.storeName}</span>}
            {extracted.date && <span className={styles.date}>{extracted.date}</span>}
            {extracted.confidenceNotes && (
              <p className={styles.notes}>Note: {extracted.confidenceNotes}</p>
            )}
          </div>

          <div className={styles.totalRow}>
            <label className={styles.totalLabel}>Receipt Total ($)</label>
            <input
              type="number"
              step="0.01"
              className={styles.totalInput}
              value={total ?? ''}
              onChange={e => setTotal(e.target.value ? Number(e.target.value) : null)}
            />
          </div>

          <p className={styles.reviewHint}>
            Review extracted items. Fill in Category for each one you want to track,
            then click Save.
          </p>

          <div className={styles.tableWrapper}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Item Name</th>
                  <th>Price ($)</th>
                  <th>Category</th>
                  <th>Serving (g)</th>
                  <th>Servings/Wk</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {items.map((row, i) => (
                  <tr key={i}>
                    <td>
                      <input
                        className={styles.input}
                        value={row.name}
                        onChange={e => updateItem(i, 'name', e.target.value)}
                      />
                    </td>
                    <td>
                      <input
                        type="number"
                        step="0.01"
                        className={`${styles.input} ${styles.narrow}`}
                        value={row.price ?? ''}
                        onChange={e => updateItem(i, 'price', e.target.value ? Number(e.target.value) : null)}
                      />
                    </td>
                    <td>
                      <select
                        className={styles.select}
                        value={row.category}
                        onChange={e => updateItem(i, 'category', e.target.value)}
                      >
                        {CATEGORIES.map(c => <option key={c} value={c}>{c || '— select —'}</option>)}
                      </select>
                    </td>
                    <td>
                      <input
                        type="number"
                        className={`${styles.input} ${styles.narrow}`}
                        value={row.servingSizeG}
                        onChange={e => updateItem(i, 'servingSizeG', e.target.value)}
                      />
                    </td>
                    <td>
                      <input
                        type="number"
                        className={`${styles.input} ${styles.narrow}`}
                        value={row.servingsPerWeek}
                        onChange={e => updateItem(i, 'servingsPerWeek', e.target.value)}
                      />
                    </td>
                    <td>
                      <button className={styles.btnRemove} onClick={() => removeRow(i)}>×</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className={styles.actions}>
            <button className={styles.btnAdd} onClick={addRow}>+ Add Row</button>
            <button
              className={styles.btnSave}
              onClick={handleConfirm}
              disabled={saving}
            >
              {saving ? 'Saving…' : 'Save & Analyze'}
            </button>
          </div>
        </>
      )}
    </div>
  )
}
