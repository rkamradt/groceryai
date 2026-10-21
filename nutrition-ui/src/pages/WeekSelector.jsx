import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getWeeks, createWeek } from '../api/nutritionApi'
import styles from './WeekSelector.module.css'

function formatDate(iso) {
  return new Date(iso).toLocaleDateString('en-US', {
    weekday: 'short', year: 'numeric', month: 'short', day: 'numeric',
  })
}

export default function WeekSelector() {
  const navigate = useNavigate()
  const [weeks, setWeeks] = useState([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    getWeeks()
      .then(setWeeks)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  async function handleNewWeek() {
    setCreating(true)
    setError(null)
    try {
      const today = new Date().toISOString().split('T')[0]
      const week = await createWeek(today, 2)
      navigate(`/entry/${week.id}`)
    } catch (e) {
      setError(e.message)
      setCreating(false)
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.topBar}>
        <h1 className={styles.title}>Your Weeks</h1>
        <button
          className={styles.btnPrimary}
          onClick={handleNewWeek}
          disabled={creating}
        >
          {creating ? 'Creating…' : '+ Start New Week'}
        </button>
      </div>

      {error && <p className={styles.error}>{error}</p>}

      {loading ? (
        <p className={styles.hint}>Loading…</p>
      ) : weeks.length === 0 ? (
        <div className={styles.empty}>
          <p>No weeks yet. Start your first week to track grocery nutrition.</p>
        </div>
      ) : (
        <ul className={styles.list}>
          {weeks.map(w => (
            <li
              key={w.id}
              className={styles.item}
              onClick={() => navigate(`/report/${w.id}`)}
            >
              <div className={styles.itemDate}>{formatDate(w.startDate)}</div>
              <div className={styles.itemMeta}>{w.people} {w.people === 1 ? 'person' : 'people'}</div>
              <span className={styles.arrow}>→</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
