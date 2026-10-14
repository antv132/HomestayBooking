// ─── SCR-61: Occupancy Report — redirect to live SCR-44 Property Reports ───────
import { Navigate } from 'react-router-dom';

/**
 * Legacy mock route `/manager/reports/occupancy`.
 * Live occupancy lives on Property Reports (tab occupancy).
 */
export function OccupancyReportPage() {
  return <Navigate to="/manager/reports/property?tab=occupancy" replace />;
}
