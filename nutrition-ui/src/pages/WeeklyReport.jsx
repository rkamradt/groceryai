import { useEffect, useState, useMemo } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getNutritionReport, exportToSheets } from '../api/nutritionApi'
import styles from './WeeklyReport.module.css'

const MACROS = [
  { key: 'protein', label: 'Protein', color: '#4a90d9', kcalPerG: 4 },
  { key: 'fat',     label: 'Fat',     color: '#e67e22', kcalPerG: 9 },
  { key: 'carbs',   label: 'Carbs',   color: '#27ae60', kcalPerG: 4 },
]

function SummaryCard({ label, value, unit, sub, prefix }) {
  return (
    <div className={styles.card}>
      <div className={styles.cardLabel}>{label}</div>
      <div className={styles.cardValue}>
        {prefix && <span className={styles.cardUnit}>{prefix}</span>}
        {value.toLocaleString(undefined, { maximumFractionDigits: 2 })}
        {unit && <span className={styles.cardUnit}> {unit}</span>}
      </div>
      {sub && <div className={styles.cardSub}>{sub}</div>}
    </div>
  )
}

function MacroBar({ weekly }) {
  const totals = MACROS.map(m => ({ ...m, kcal: weekly[m.key] * m.kcalPerG }))
  const totalKcal = totals.reduce((s, m) => s + m.kcal, 0)
  if (totalKcal === 0) return null
  return (
    <div>
      <div className={styles.macroBarTrack}>
        {totals.map(m => (
          <div
            key={m.key}
            className={styles.macroBarSegment}
            style={{ width: `${(m.kcal / totalKcal) * 100}%`, background: m.color }}
            title={`${m.label}: ${((m.kcal / totalKcal) * 100).toFixed(1)}%`}
          />
        ))}
      </div>
      <div className={styles.macroLegend}>
        {totals.map(m => (
          <span key={m.key} className={styles.legendItem}>
            <span className={styles.legendDot} style={{ background: m.color }} />
            {m.label} {((m.kcal / totalKcal) * 100).toFixed(1)}%
          </span>
        ))}
      </div>
    </div>
  )
}

function CategoryChart({ categoryCalories }) {
  const maxCal = Math.max(...categoryCalories.map(c => c.calories), 1)
  return (
    <div className={styles.chart}>
      {categoryCalories.map(c => (
        <div key={c.category} className={styles.chartRow}>
          <div className={styles.chartLabel}>{c.category}</div>
          <div className={styles.chartBarTrack}>
            <div className={styles.chartBar} style={{ width: `${(c.calories / maxCal) * 100}%` }} />
          </div>
          <div className={styles.chartValue}>{c.calories.toLocaleString()} kcal</div>
        </div>
      ))}
    </div>
  )
}

const COLS = [
  { key: 'name',           label: 'Item' },
  { key: 'category',      label: 'Category' },
  { key: 'price',         label: 'Price ($)' },
  { key: 'weeklyCalories', label: 'Cal (kcal)' },
  { key: 'weeklyProtein', label: 'Protein (g)' },
  { key: 'weeklyFat',     label: 'Fat (g)' },
  { key: 'weeklyCarbs',   label: 'Carbs (g)' },
  { key: 'weeklyFiber',   label: 'Fiber (g)' },
  { key: 'weeklySodium',  label: 'Sodium (mg)' },
]

export default function WeeklyReport() {
  const { weekId } = useParams()
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [sortKey, setSortKey] = useState('weeklyCalories')
  const [sortAsc, setSortAsc] = useState(false)
  const [filterCat, setFilterCat] = useState('All')
  const [exporting, setExporting] = useState(false)
  const [exportMsg, setExportMsg] = useState(null)

  useEffect(() => {
    getNutritionReport(weekId)
      .then(setReport)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [weekId])

  const categories = useMemo(() => {
    if (!report) return []
    return ['All', ...new Set(report.items.map(i => i.category))]
  }, [report])

  const filteredItems = useMemo(() => {
    if (!report) return []
    const items = filterCat === 'All' ? report.items : report.items.filter(i => i.category === filterCat)
    return [...items].sort((a, b) => {
      const av = a[sortKey] ?? 0
      const bv = b[sortKey] ?? 0
      if (av < bv) return sortAsc ? -1 : 1
      if (av > bv) return sortAsc ? 1 : -1
      return 0
    })
  }, [report, sortKey, sortAsc, filterCat])

  function toggleSort(key) {
    if (sortKey === key) setSortAsc(v => !v)
    else { setSortKey(key); setSortAsc(true) }
  }

  async function handleExport() {
    setExporting(true)
    setExportMsg(null)
    try {
      const msg = await exportToSheets(weekId)
      setExportMsg(msg || 'Exported to Google Sheets.')
    } catch (e) {
      setExportMsg('Export failed: ' + e.message)
    } finally {
      setExporting(false)
    }
  }

  if (loading) return <p className={styles.hint}>Loading report…</p>
  if (error) return <p className={styles.error}>{error}</p>
  if (!report) return null

  const { weeklyTotal: wt, perPersonPerDay: ppd, categoryCalories, people, totalCost, perPersonCost } = report

  return (
    <div className={styles.container}>
      <div className={styles.topBar}>
        <div>
          <Link to="/" className={styles.back}>← All Weeks</Link>
          <h1 className={styles.title}>Weekly Report</h1>
        </div>
        <div className={styles.topBarActions}>
          <Link to={`/upload/${weekId}`} className={styles.btnUpload}>+ Upload Receipt</Link>
          <button className={styles.btnExport} onClick={handleExport} disabled={exporting}>
            {exporting ? 'Exporting…' : 'Export to Sheets'}
          </button>
        </div>
      </div>

      {exportMsg && <p className={styles.info}>{exportMsg}</p>}

      {/* Cost summary */}
      {totalCost != null && (
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Budget</h2>
          <div className={styles.cards}>
            <SummaryCard label="Total Spent" value={totalCost} prefix="$" sub="This week's grocery receipt" />
            {perPersonCost != null && (
              <SummaryCard label="Per Person" value={perPersonCost} prefix="$"
                sub={`${people} ${people === 1 ? 'person' : 'people'}`} />
            )}
          </div>
        </section>
      )}

      {/* Nutrition summary */}
      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>Weekly Totals</h2>
        <p className={styles.sectionSub}>Sub-label shows per-person / day ({people} {people === 1 ? 'person' : 'people'})</p>
        <div className={styles.cards}>
          <SummaryCard label="Calories" value={wt.calories} unit="kcal" sub={`${ppd.calories} kcal / person / day`} />
          <SummaryCard label="Protein"  value={wt.protein}  unit="g"    sub={`${ppd.protein} g / person / day`} />
          <SummaryCard label="Fat"      value={wt.fat}      unit="g"    sub={`${ppd.fat} g / person / day`} />
          <SummaryCard label="Carbs"    value={wt.carbs}    unit="g"    sub={`${ppd.carbs} g / person / day`} />
          <SummaryCard label="Fiber"    value={wt.fiber}    unit="g"    sub={`${ppd.fiber} g / person / day`} />
          <SummaryCard label="Sodium"   value={wt.sodium}   unit="mg"   sub={`${ppd.sodium} mg / person / day`} />
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionTitle}>Macro Calorie Distribution</h2>
        <MacroBar weekly={wt} />
      </section>

      {categoryCalories.length > 0 && (
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Calories by Category</h2>
          <CategoryChart categoryCalories={categoryCalories} />
        </section>
      )}

      <section className={styles.section}>
        <div className={styles.tableTopBar}>
          <h2 className={styles.sectionTitle}>Items</h2>
          <select className={styles.catFilter} value={filterCat} onChange={e => setFilterCat(e.target.value)}>
            {categories.map(c => <option key={c}>{c}</option>)}
          </select>
        </div>

        <div className={styles.tableWrapper}>
          <table className={styles.table}>
            <thead>
              <tr>
                {COLS.map(c => (
                  <th key={c.key} onClick={() => toggleSort(c.key)} className={styles.th}>
                    {c.label}{sortKey === c.key ? (sortAsc ? ' ▲' : ' ▼') : ''}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((item, i) => (
                <tr key={i} className={i % 2 === 1 ? styles.alt : ''}>
                  <td>{item.name}</td>
                  <td>{item.category}</td>
                  <td>{item.price != null ? `$${item.price.toFixed(2)}` : '—'}</td>
                  <td>{item.weeklyCalories.toLocaleString()}</td>
                  <td>{item.weeklyProtein}</td>
                  <td>{item.weeklyFat}</td>
                  <td>{item.weeklyCarbs}</td>
                  <td>{item.weeklyFiber}</td>
                  <td>{item.weeklySodium.toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
