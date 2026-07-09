'use client';

import { useState, useEffect, useRef } from 'react';
import dynamic from 'next/dynamic';
import 'leaflet/dist/leaflet.css';

type Area = { id: string; name: string; isDanger: number };
type Loto = { id: string; areaId: string; text: string; areaName?: string };
type Checklist = { id: string; text: string };
type User = { employeeId: string; name: string };

type ModalType = 'area' | 'loto' | 'checklist' | 'user' | null;
type TabType = 'single' | 'batch' | 'csv';

// Map component (client only)
const MapComponent = dynamic(() => import('@/app/components/MapComponent'), {
  ssr: false,
  loading: () => <div style={{ height: '400px', backgroundColor: 'rgba(0,0,0,0.4)' }}>지도 로딩 중...</div>
});

export default function ManagePage() {
  // Data lists
  const [areas, setAreas] = useState<Area[]>([]);
  const [lotos, setLotos] = useState<Loto[]>([]);
  const [checklists, setChecklists] = useState<Checklist[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  
  const [loading, setLoading] = useState(true);

  // Modal control
  const [activeModal, setActiveModal] = useState<ModalType>(null);
  const [activeTab, setActiveTab] = useState<TabType>('single');

  // Single Add form states
  const [areaId, setAreaId] = useState('');
  const [areaName, setAreaName] = useState('');
  const [areaIsDanger, setAreaIsDanger] = useState(false);

  const [lotoId, setLotoId] = useState('');
  const [lotoAreaId, setLotoAreaId] = useState('');
  const [lotoText, setLotoText] = useState('');

  const [checklistId, setChecklistId] = useState('');
  const [checklistText, setChecklistText] = useState('');

  const [employeeId, setEmployeeId] = useState('');
  const [employeeName, setEmployeeName] = useState('');

  // Batch / CSV states
  const [batchText, setBatchText] = useState('');
  const [csvFile, setCsvFile] = useState<File | null>(null);

  // Global Message
  const [message, setMessage] = useState({ text: '', isError: false });

  const showMessage = (text: string, isError = false) => {
    setMessage({ text, isError });
    setTimeout(() => setMessage({ text: '', isError: false }), 4000);
  };

  // Test location reporting
  const testReportLocation = async () => {
    try {
      const employees = users || [];
      if (employees.length === 0) {
        showMessage('먼저 사원을 등록해주세요.', true);
        return;
      }

      // Report random locations for all employees
      const promises = employees.map(emp =>
        fetch('/api/location/report', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            employeeId: emp.employeeId,
            latitude: 35.1595 + (Math.random() - 0.5) * 0.1,
            longitude: 129.0430 + (Math.random() - 0.5) * 0.1
          })
        })
      );

      await Promise.all(promises);
      showMessage(`${employees.length}명의 위치 정보가 등록되었습니다.`);
    } catch (err: any) {
      showMessage('위치 등록에 실패했습니다.', true);
    }
  };

  const fetchData = async () => {
    setLoading(true);
    try {
      const [areasRes, lotosRes, checklistsRes, usersRes] = await Promise.all([
        fetch('/api/areas'),
        fetch('/api/lotos'),
        fetch('/api/checklists'),
        fetch('/api/users')
      ]);

      const [areasData, lotosData, checklistsData, usersData] = await Promise.all([
        areasRes.json(),
        lotosRes.json(),
        checklistsRes.json(),
        usersRes.json()
      ]);

      if (areasData.success) setAreas(areasData.areas);
      if (lotosData.success) setLotos(lotosData.lotos);
      if (checklistsData.success) setChecklists(checklistsData.checklists);
      if (usersData.success) setUsers(usersData.users);

      if (areasData.areas?.length > 0) {
        setLotoAreaId(areasData.areas[0].id);
      }
    } catch (err) {
      console.error('Error fetching data', err);
      showMessage('데이터를 로드하는 데 실패했습니다.', true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const closeModal = () => {
    setActiveModal(null);
    setActiveTab('single');
    setBatchText('');
    setCsvFile(null);
    // Reset forms
    setAreaId('');
    setAreaName('');
    setAreaIsDanger(false);
    setLotoId('');
    setLotoText('');
    setChecklistId('');
    setChecklistText('');
    setEmployeeId('');
    setEmployeeName('');
  };

  const handleDelete = async (type: 'areas' | 'lotos' | 'checklists' | 'users', idValue: string) => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    const bodyKey = type === 'users' ? 'employeeId' : 'id';
    
    try {
      const res = await fetch(`/api/${type}/delete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ [bodyKey]: idValue })
      });
      const data = await res.json();
      if (data.success) {
        showMessage('성공적으로 삭제되었습니다.');
        fetchData();
      } else {
        showMessage(data.message || '삭제에 실패했습니다.', true);
      }
    } catch (err: any) {
      showMessage(err.message, true);
    }
  };

  // Parsing utilities
  const parseRows = (text: string) => {
    return text.split('\n')
      .map(line => line.split(/[,\t]/).map(val => val.trim()))
      .filter(row => row.length > 0 && row[0] !== '');
  };

  const handleBatchSubmit = async (e: React.FormEvent, type: ModalType) => {
    e.preventDefault();
    let rawText = '';

    if (activeTab === 'batch') {
      rawText = batchText;
    } else if (activeTab === 'csv' && csvFile) {
      const reader = new FileReader();
      reader.onload = async (event) => {
        const text = event.target?.result as string;
        await processBatchData(text, type);
      };
      reader.readAsText(csvFile);
      return;
    } else {
      showMessage('입력 방식을 확인하세요.', true);
      return;
    }

    await processBatchData(rawText, type);
  };

  const processBatchData = async (text: string, type: ModalType) => {
    const rows = parseRows(text);
    if (rows.length === 0) {
      showMessage('파싱할 데이터가 없습니다.', true);
      return;
    }

    let endpoint = `/api/${type === 'user' ? 'users' : type + 's'}/batch-add`;
    let items: any[] = [];

    try {
      if (type === 'area') {
        items = rows.map(row => ({
          id: row[0],
          name: row[1],
          isDanger: row[2] === '1' || row[2]?.toLowerCase() === 'true'
        }));
      } else if (type === 'loto') {
        items = rows.map(row => ({
          id: row[0],
          areaId: row[1],
          text: row[2]
        }));
      } else if (type === 'checklist') {
        items = rows.map(row => ({
          id: row[0],
          text: row[1]
        }));
      } else if (type === 'user') {
        items = rows.map(row => ({
          employeeId: row[0],
          name: row[1]
        }));
      }

      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ items })
      });
      const data = await res.json();
      if (data.success) {
        showMessage(`${items.length}개의 데이터가 성공적으로 등록되었습니다.`);
        closeModal();
        fetchData();
      } else {
        showMessage(data.message || '일괄 추가에 실패했습니다.', true);
      }
    } catch (err: any) {
      showMessage('데이터 포맷이 올바르지 않습니다. 가이드를 확인하세요.', true);
    }
  };

  const handleSingleSubmit = async (e: React.FormEvent, type: ModalType) => {
    e.preventDefault();
    let body = {};
    let endpoint = '';

    if (type === 'area') {
      if (!areaId || !areaName) return showMessage('필드를 모두 채워주세요.', true);
      body = { id: areaId, name: areaName, isDanger: areaIsDanger };
      endpoint = '/api/areas/add';
    } else if (type === 'loto') {
      if (!lotoId || !lotoAreaId || !lotoText) return showMessage('필드를 모두 채워주세요.', true);
      body = { id: lotoId, areaId: lotoAreaId, text: lotoText };
      endpoint = '/api/lotos/add';
    } else if (type === 'checklist') {
      if (!checklistId || !checklistText) return showMessage('필드를 모두 채워주세요.', true);
      body = { id: checklistId, text: checklistText };
      endpoint = '/api/checklists/add';
    } else if (type === 'user') {
      if (!employeeId || !employeeName) return showMessage('필드를 모두 채워주세요.', true);
      body = { employeeId, name: employeeName };
      endpoint = '/api/users/add';
    }

    try {
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });
      const data = await res.json();
      if (data.success) {
        showMessage('성공적으로 추가되었습니다.');
        closeModal();
        fetchData();
      } else {
        showMessage(data.message || '추가에 실패했습니다.', true);
      }
    } catch (err: any) {
      showMessage(err.message, true);
    }
  };

  return (
    <div className="dashboard-container">
      <header className="header">
        <h1>Safety Site - 데이터 통합 관리 센터</h1>
      </header>

      {message.text && (
        <div className={`toast-message ${message.isError ? 'error' : 'success'}`}>
          {message.text}
        </div>
      )}

      {loading ? (
        <div className="empty-text">데이터를 불러오는 중입니다...</div>
      ) : (
        <>
          {/* Realtime GPS Map Card */}
          <section className="card" style={{ marginBottom: '2rem', minHeight: '480px' }}>
            <div className="card-header" style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '1rem', marginBottom: '1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <h2>🗺️ 실시간 작업자 GPS 위치 관제</h2>
                <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>* 5초마다 자동 갱신됩니다.</span>
              </div>
              <button className="btn-add" onClick={testReportLocation} style={{ whiteSpace: 'nowrap', marginLeft: '1rem' }}>테스트 위치 (+)</button>
            </div>
            <MapComponent />
          </section>

          <main className="manage-grid">
          {/* Areas Table */}
          <section className="card">
            <div className="card-header">
              <h2>🚧 작업구역 목록</h2>
              <button className="btn-add" onClick={() => setActiveModal('area')}>추가 (+)</button>
            </div>
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>구역명</th>
                    <th>위험여부</th>
                    <th>작업</th>
                  </tr>
                </thead>
                <tbody>
                  {areas.length === 0 ? (
                    <tr><td colSpan={4} className="empty-text">등록된 구역이 없습니다.</td></tr>
                  ) : (
                    areas.map(area => (
                      <tr key={area.id}>
                        <td className="nowrap">{area.id}</td>
                        <td className="nowrap">{area.name}</td>
                        <td className={(area.isDanger ? 'text-danger fw-bold' : '') + ' nowrap'}>
                          {area.isDanger ? '⚠️ 위험' : '일반'}
                        </td>
                        <td className="nowrap">
                          <button className="btn-delete" onClick={() => handleDelete('areas', area.id)}>삭제</button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>

          {/* Lotos Table */}
          <section className="card">
            <div className="card-header">
              <h2>🔒 LOTO 목록</h2>
              <button className="btn-add" onClick={() => setActiveModal('loto')}>추가 (+)</button>
            </div>
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>작업구역</th>
                    <th>조치 내용</th>
                    <th>QR 코드</th>
                    <th>작업</th>
                  </tr>
                </thead>
                <tbody>
                  {lotos.length === 0 ? (
                    <tr><td colSpan={5} className="empty-text">등록된 LOTO가 없습니다.</td></tr>
                  ) : (
                    lotos.map(loto => (
                      <tr key={loto.id}>
                        <td className="nowrap">{loto.id}</td>
                        <td className="nowrap">{loto.areaName || loto.areaId}</td>
                        <td style={{ minWidth: '150px' }}>{loto.text}</td>
                        <td className="nowrap">
                          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px', padding: '4px' }}>
                            <img 
                              src={`https://api.qrserver.com/v1/create-qr-code/?size=80x80&data=${encodeURIComponent(loto.id)}`} 
                              alt="QR" 
                              width={80} 
                              height={80} 
                              style={{ border: '1px solid #ccc', padding: '2px', background: '#fff', borderRadius: '4px' }}
                            />
                            <span style={{ fontSize: '11px', color: '#666', fontWeight: 'bold' }}>{loto.id}</span>
                          </div>
                        </td>
                        <td className="nowrap">
                          <button className="btn-delete" onClick={() => handleDelete('lotos', loto.id)}>삭제</button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>

          {/* Checklists Table */}
          <section className="card">
            <div className="card-header">
              <h2>📝 최종 체크리스트</h2>
              <button className="btn-add" onClick={() => setActiveModal('checklist')}>추가 (+)</button>
            </div>
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>점검 항목</th>
                    <th>작업</th>
                  </tr>
                </thead>
                <tbody>
                  {checklists.length === 0 ? (
                    <tr><td colSpan={3} className="empty-text">등록된 체크리스트가 없습니다.</td></tr>
                  ) : (
                    checklists.map(chk => (
                      <tr key={chk.id}>
                        <td>{chk.id}</td>
                        <td>{chk.text}</td>
                        <td>
                          <button className="btn-delete" onClick={() => handleDelete('checklists', chk.id)}>삭제</button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>

          {/* Users Table */}
          <section className="card">
            <div className="card-header">
              <h2>👤 사원 목록</h2>
              <button className="btn-add" onClick={() => setActiveModal('user')}>추가 (+)</button>
            </div>
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>사원번호</th>
                    <th>이름</th>
                    <th>작업</th>
                  </tr>
                </thead>
                <tbody>
                  {users.length === 0 ? (
                    <tr><td colSpan={3} className="empty-text">등록된 사원이 없습니다.</td></tr>
                  ) : (
                    users.map(u => (
                      <tr key={u.employeeId}>
                        <td>{u.employeeId}</td>
                        <td>{u.name}</td>
                        <td>
                          <button className="btn-delete" onClick={() => handleDelete('users', u.employeeId)}>삭제</button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </main>
      </>
    )}

      {/* Popups / Modals */}
      {activeModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>
                {activeModal === 'area' && '🚧 작업구역 추가'}
                {activeModal === 'loto' && '🔒 LOTO 안전조치 추가'}
                {activeModal === 'checklist' && '📝 최종 체크리스트 추가'}
                {activeModal === 'user' && '👤 사원(작업자) 추가'}
              </h2>
              <button className="btn-close" onClick={closeModal}>&times;</button>
            </div>

            {/* Modal Tabs */}
            <div className="modal-tabs">
              <button className={`tab-btn ${activeTab === 'single' ? 'active' : ''}`} onClick={() => setActiveTab('single')}>개별 추가</button>
              <button className={`tab-btn ${activeTab === 'batch' ? 'active' : ''}`} onClick={() => setActiveTab('batch')}>일괄 추가 (텍스트)</button>
              <button className={`tab-btn ${activeTab === 'csv' ? 'active' : ''}`} onClick={() => setActiveTab('csv')}>CSV 업로드</button>
            </div>

            <div className="modal-body">
              {/* Tab: Single Add */}
              {activeTab === 'single' && (
                <form onSubmit={e => handleSingleSubmit(e, activeModal)} className="manage-form">
                  {activeModal === 'area' && (
                    <>
                      <div className="input-group">
                        <label>구역 식별 ID</label>
                        <input type="text" placeholder="예: area_3" value={areaId} onChange={e => setAreaId(e.target.value)} required />
                      </div>
                      <div className="input-group">
                        <label>구역명</label>
                        <input type="text" placeholder="예: 공학4관 전산실" value={areaName} onChange={e => setAreaName(e.target.value)} required />
                      </div>
                      <div className="input-group checkbox-group">
                        <label>
                          <input type="checkbox" checked={areaIsDanger} onChange={e => setAreaIsDanger(e.target.checked)} />
                          위험 지역 여부 (고위험 작업장)
                        </label>
                      </div>
                    </>
                  )}

                  {activeModal === 'loto' && (
                    <>
                      <div className="input-group">
                        <label>LOTO 식별 ID</label>
                        <input type="text" placeholder="예: loto_3" value={lotoId} onChange={e => setLotoId(e.target.value)} required />
                      </div>
                      <div className="input-group">
                        <label>해당 작업구역</label>
                        <select value={lotoAreaId} onChange={e => setLotoAreaId(e.target.value)} required>
                          {areas.map(area => (
                            <option key={area.id} value={area.id}>{area.name} ({area.id})</option>
                          ))}
                        </select>
                      </div>
                      <div className="input-group">
                        <label>안전조치 내용 (QR 텍스트)</label>
                        <input type="text" placeholder="예: 메인 가스 밸브 잠금" value={lotoText} onChange={e => setLotoText(e.target.value)} required />
                      </div>
                    </>
                  )}

                  {activeModal === 'checklist' && (
                    <>
                      <div className="input-group">
                        <label>체크리스트 식별 ID</label>
                        <input type="text" placeholder="예: chk_3" value={checklistId} onChange={e => setChecklistId(e.target.value)} required />
                      </div>
                      <div className="input-group">
                        <label>점검 항목 내용</label>
                        <input type="text" placeholder="예: 안전모 턱끈을 조였나요?" value={checklistText} onChange={e => setChecklistText(e.target.value)} required />
                      </div>
                    </>
                  )}

                  {activeModal === 'user' && (
                    <>
                      <div className="input-group">
                        <label>사원 번호</label>
                        <input type="text" placeholder="예: 55555" value={employeeId} onChange={e => setEmployeeId(e.target.value)} required />
                      </div>
                      <div className="input-group">
                        <label>사원 이름</label>
                        <input type="text" placeholder="예: 김안전" value={employeeName} onChange={e => setEmployeeName(e.target.value)} required />
                      </div>
                    </>
                  )}

                  <button type="submit" className="btn-submit">추가</button>
                </form>
              )}

              {/* Tab: Batch Add (Textarea) */}
              {activeTab === 'batch' && (
                <form onSubmit={e => handleBatchSubmit(e, activeModal)} className="manage-form">
                  <div className="input-group">
                    <label>
                      데이터 형식 가이드 (줄바꿈으로 구분, 쉼표 또는 탭으로 열 구분)
                    </label>
                    <div className="format-guide">
                      {activeModal === 'area' && 'ID, 구역명, 위험여부(1 또는 0)\n예시:\narea_3,공학4관 전산실,0\narea_4,화학2관 정제소,1'}
                      {activeModal === 'loto' && 'ID, 작업구역ID, 조치내용\n예시:\nloto_3,area_2,압력 밸브 오프\nloto_4,area_1,안전망 고정'}
                      {activeModal === 'checklist' && 'ID, 체크항목 내용\n예시:\nchk_3,산소 농도를 측정했습니까?\nchk_4,접지 상태를 확인했습니까?'}
                      {activeModal === 'user' && '사원번호, 이름\n예시:\n55555,김코딩\n66666,이백엔'}
                    </div>
                    <textarea
                      placeholder="데이터를 붙여넣으세요..."
                      value={batchText}
                      onChange={e => setBatchText(e.target.value)}
                      rows={6}
                      className="batch-textarea"
                      required
                    />
                  </div>
                  <button type="submit" className="btn-submit">일괄 등록</button>
                </form>
              )}

              {/* Tab: CSV Upload */}
              {activeTab === 'csv' && (
                <form onSubmit={e => handleBatchSubmit(e, activeModal)} className="manage-form">
                  <div className="input-group">
                    <label>CSV 파일 선택 (.csv)</label>
                    <div className="format-guide">
                      데이터 형식은 일괄 텍스트 추가와 동일해야 합니다. (첫째 줄 헤더 없음)
                    </div>
                    <input
                      type="file"
                      accept=".csv"
                      onChange={e => setCsvFile(e.target.files?.[0] || null)}
                      className="csv-file-input"
                      required
                    />
                  </div>
                  <button type="submit" className="btn-submit" disabled={!csvFile}>CSV 업로드</button>
                </form>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
