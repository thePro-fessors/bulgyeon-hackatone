'use client';

import { useEffect, useState } from 'react';

type WorkLog = {
  id: number;
  employeeId: string;
  areaId: string;
  startTime: string;
  durationMinutes: number;
  isFinished: number;
  userName: string;
  areaName: string;
};

type Accident = {
  id: number;
  employeeId: string;
  type: string;
  timestamp: string;
  userName: string;
};

export default function AdminDashboard() {
  const [activeLogs, setActiveLogs] = useState<WorkLog[]>([]);
  const [accidents, setAccidents] = useState<Accident[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await fetch('/api/admin/dashboard');
        const data = await res.json();
        if (data.success) {
          setActiveLogs(data.activeLogs);
          setAccidents(data.accidents);
        }
      } catch (err) {
        console.error('Failed to fetch dashboard data', err);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, 1000); // Polling every 1 seconds
    return () => clearInterval(interval);
  }, []);

  const calculateRemainingTime = (log: WorkLog) => {
    const start = new Date(log.startTime).getTime();
    const durationMs = log.durationMinutes * 60 * 1000;
    const end = start + durationMs;
    const now = new Date().getTime();
    const remainingMs = end - now;

    if (remainingMs <= 0) return '시간 초과!';
    
    const h = Math.floor(remainingMs / (1000 * 60 * 60));
    const m = Math.floor((Math.abs(remainingMs) % (1000 * 60 * 60)) / (1000 * 60));
    const s = Math.floor((Math.abs(remainingMs) % (1000 * 60)) / 1000);
    
    return `${h.toString().padStart(2, '0')} : ${m.toString().padStart(2, '0')} : ${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="dashboard-container">
      <header className="header">
        <h1>Safety Site - 중앙 관제 대시보드</h1>
      </header>
      
      <main className="main-content">
        <section className="card">
          <h2>🚧 현재 작업 인원 및 구역</h2>
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>구역명</th>
                  <th>사원명(사번)</th>
                  <th>시작 시간</th>
                  <th>남은 시간</th>
                </tr>
              </thead>
              <tbody>
                {activeLogs.length === 0 ? (
                  <tr><td colSpan={4} className="empty-text">현재 투입된 인원이 없습니다.</td></tr>
                ) : (
                  activeLogs.map(log => (
                    <tr key={log.id}>
                      <td>{log.areaName}</td>
                      <td>{log.userName} ({log.employeeId})</td>
                      <td>{new Date(log.startTime).toLocaleTimeString()}</td>
                      <td className={calculateRemainingTime(log) === '시간 초과!' ? 'text-danger fw-bold' : 'fw-bold'}>
                        {calculateRemainingTime(log)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="card border-danger">
          <h2 className="text-danger">⚠️ 비상 상황 / 사고 내역</h2>
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>발생 시간</th>
                  <th>사원명(사번)</th>
                  <th>비상 타입</th>
                </tr>
              </thead>
              <tbody>
                {accidents.length === 0 ? (
                  <tr><td colSpan={3} className="empty-text">발생한 사고 내역이 없습니다.</td></tr>
                ) : (
                  accidents.map(acc => (
                    <tr key={acc.id} className="row-danger">
                      <td>{new Date(acc.timestamp).toLocaleString()}</td>
                      <td>{acc.userName} ({acc.employeeId})</td>
                      <td className="fw-bold text-danger">{acc.type}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  );
}
