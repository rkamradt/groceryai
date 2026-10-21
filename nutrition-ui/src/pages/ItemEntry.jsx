import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { saveItems } from '../api/nutritionApi'
import styles from './ItemEntry.module.css'

const CATEGORIES = [
  'Proteins (Meat / Fish / Poultry)',
  'Dairy',
  'Produce – Vegetables',
  'Produce – Fruits',
  'Grains & Bread',
  'Legumes & Beans',
  'Nuts & Seeds',
  'Oils & Fats',
  'Snacks & Sweets',
  'Beverages',
]

function emptyRow() {
  return { name: '', category: CATEGORIES[0], servingSizeG: '', servingsPerWeek: '' }
}

export default function ItemEntry() {
  const { weekId } = useParams()
  const navigate = useNavigate()
  const [rows, setRows] = useState([emptyRow()])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  function updateRow(index, field, value) {
    setRows(prev => prev.map((r, i) => i === index ? { ...r, [field]: value } : r))
  }

  function addRow() {
    setRows(prev => [...prev, emptyRow()])
  }

  function removeRow(index) {
    setRows(prev => prev.filter((_, i) => i !== index))
  }

  async function handleSave() {
    const valid = rows.filter(r => r.name.trim())
    if (valid.length === 0) {
      setError('Add at least one item with a name.')
      return
    }

    const items = valid.map(r => ({
      name: r.name.trim(),
      category: r.category,
      servingSizeG: parseFloat(r.servingSizeG) || 100,
      servingsPerWeek: parseInt(r.servingsPerWeek, 10) || 1,
    }))

    setSaving(true)
    setError(null)
    try {
      await saveItems(weekId, items)
      navigate(`/report/${weekId}`)
    } catch (e) {
      setError(e.message)
      setSaving(false)
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.topBar}>
        <h1 className={styles.title}>Add Grocery Items</h1>
        <div className={styles.actions}>
          <button className={styles.btnSecondary} onClick={addRow} type="button">
            + Add Row
          </button>
          <button
            className={styles.btnPrimary}
            onClick={handleSave}
            disabled={saving}
            type="button"
          >
            {saving ? 'Saving…' : 'Save & Analyze'}
          </button>
        </div>
      </div>

      {saving && (
        <p className={styles.info}>
          Looking up nutrition data from USDA — this may take a moment…
        </p>
      )}

      {error && <p className={styles.error}>{error}</p>}

      <div className={styles.tableWrapper}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Item Name</th>
              <th>Category</th>
              <th>Serving Size (g)</th>
              <th>Servings / Week</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, i) => (
              <tr key={i} className={i % 2 === 1 ? styles.alt : ''}>
                <td>
                  <input
                    className={styles.input}
                    type="text"
                    placeholder="e.g. Chicken breast"
                    value={row.name}
                    onChange={e => updateRow(i, 'name', e.target.value)}
                  />
                </td>
                <td>
                  <select
                    className={styles.select}
                    value={row.category}
                    onChange={e => updateRow(i, 'category', e.target.value)}
                  >
                    {CATEGORIES.map(c => <option key={c}>{c}</option>)}
                  </select>
                </td>
                <td>
                  <input
                    className={styles.inputNum}
                    type="number"
                    min="1"
                    placeholder="100"
                    value={row.servingSizeG}
                    onChange={e => updateRow(i, 'servingSizeG', e.target.value)}
                  />
                </td>
                <td>
                  <input
                    className={styles.inputNum}
                    type="number"
                    min="1"
                    placeholder="1"
                    value={row.servingsPerWeek}
                    onChange={e => updateRow(i, 'servingsPerWeek', e.target.value)}
                  />
                </td>
                <td>
                  <button
                    className={styles.btnRemove}
                    onClick={() => removeRow(i)}
                    disabled={rows.length === 1}
                    type="button"
                    title="Remove row"
                  >
                    ×
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
