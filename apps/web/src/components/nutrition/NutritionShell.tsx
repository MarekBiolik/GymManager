import { NavLink, Navigate, Outlet, Route, Routes } from "react-router-dom";
import { HelloSyncPanel } from "../HelloSyncPanel";
import { ChartsPanel } from "./ChartsPanel";
import { DiaryPanel } from "./DiaryPanel";
import { FoodsPanel } from "./FoodsPanel";
import { NutritionSettingsPanel } from "./NutritionSettingsPanel";

type Props = {
  userId: string;
  onSignOut: () => void;
  onAccountDeleted: () => void;
};

function navClass({ isActive }: { isActive: boolean }): string {
  return isActive ? "nutrition-nav-link is-active" : "nutrition-nav-link";
}

function ShellLayout({ onSignOut }: Pick<Props, "onSignOut">) {
  return (
    <div className="nutrition-shell">
      <header className="nutrition-header">
        <div>
          <h1 className="nutrition-brand">GymManager</h1>
          <p className="nutrition-sub">Nutrition</p>
        </div>
        <button type="button" className="auth-button auth-button--secondary" onClick={onSignOut}>
          Sign out
        </button>
      </header>
      <nav className="nutrition-nav" aria-label="Nutrition">
        <NavLink to="/diary" className={navClass}>
          Diary
        </NavLink>
        <NavLink to="/foods" className={navClass}>
          Foods
        </NavLink>
        <NavLink to="/charts" className={navClass}>
          Charts
        </NavLink>
        <NavLink to="/settings" className={navClass}>
          Settings
        </NavLink>
      </nav>
      <div className="nutrition-main">
        <Outlet />
      </div>
    </div>
  );
}

export function NutritionApp({ userId, onSignOut, onAccountDeleted }: Props) {
  return (
    <Routes>
      <Route element={<ShellLayout onSignOut={onSignOut} />}>
        <Route index element={<Navigate to="/diary" replace />} />
        <Route path="/diary" element={<DiaryPanel userId={userId} />} />
        <Route path="/foods" element={<FoodsPanel userId={userId} />} />
        <Route path="/charts" element={<ChartsPanel userId={userId} />} />
        <Route
          path="/settings"
          element={
            <>
              <NutritionSettingsPanel userId={userId} onAccountDeleted={onAccountDeleted} />
              <HelloSyncPanel userId={userId} />
            </>
          }
        />
        <Route path="*" element={<Navigate to="/diary" replace />} />
      </Route>
    </Routes>
  );
}
