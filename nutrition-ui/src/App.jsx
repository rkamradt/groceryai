import { useEffect, useState } from 'react'
import { Routes, Route } from 'react-router-dom'
import WeekSelector from './pages/WeekSelector'
import ItemEntry from './pages/ItemEntry'
import WeeklyReport from './pages/WeeklyReport'
import ReceiptUpload from './pages/ReceiptUpload'
import { getMe, signIn, signOut } from './api/nutritionApi'
import styles from './App.module.css'

export default function App() {
  const [user, setUser] = useState(null)

  useEffect(() => {
    getMe().then(setUser).catch(() => setUser({ signedIn: false }))
  }, [])

  return (
    <div className={styles.app}>
      <header className={styles.header}>
        <div className={styles.headerLeft}>
          <span className={styles.logo}>GroceryAI</span>
          <span className={styles.tagline}>Weekly Nutrition Tracker</span>
        </div>
        <div className={styles.headerRight}>
          {user?.signedIn ? (
            <>
              {user.picture && (
                <img className={styles.avatar} src={user.picture} alt={user.name} />
              )}
              <span className={styles.userName}>{user.name}</span>
              <button className={styles.btnSignOut} onClick={signOut}>Sign out</button>
            </>
          ) : (
            <button className={styles.btnSignIn} onClick={signIn}>
              Sign in with Google
            </button>
          )}
        </div>
      </header>
      <main className={styles.main}>
        <Routes>
          <Route path="/" element={<WeekSelector />} />
          <Route path="/entry/:weekId" element={<ItemEntry />} />
          <Route path="/report/:weekId" element={<WeeklyReport />} />
          <Route path="/upload/:weekId" element={<ReceiptUpload />} />
        </Routes>
      </main>
    </div>
  )
}
