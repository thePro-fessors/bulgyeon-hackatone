import sqlite3 from 'sqlite3';
import { open } from 'sqlite';
import path from 'path';

async function seed() {
  const dbPath = path.join(process.cwd(), 'safety.db');
  const db = await open({
    filename: dbPath,
    driver: sqlite3.Database
  });

  await db.exec(`
    CREATE TABLE IF NOT EXISTS Areas (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      isDanger INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS Lotos (
      id TEXT PRIMARY KEY,
      areaId TEXT NOT NULL,
      text TEXT NOT NULL,
      FOREIGN KEY(areaId) REFERENCES Areas(id)
    );

    CREATE TABLE IF NOT EXISTS Checklists (
      id TEXT PRIMARY KEY,
      text TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS Users (
      employeeId TEXT PRIMARY KEY,
      name TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS WorkLogs (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      employeeId TEXT NOT NULL,
      areaId TEXT NOT NULL,
      startTime TEXT NOT NULL,
      durationMinutes INTEGER NOT NULL,
      isFinished INTEGER DEFAULT 0,
      FOREIGN KEY(employeeId) REFERENCES Users(employeeId),
      FOREIGN KEY(areaId) REFERENCES Areas(id)
    );

    CREATE TABLE IF NOT EXISTS Accidents (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      employeeId TEXT NOT NULL,
      type TEXT NOT NULL,
      timestamp TEXT NOT NULL,
      FOREIGN KEY(employeeId) REFERENCES Users(employeeId)
    );
  `);

  // Insert dummy data
  await db.run('DELETE FROM Areas');
  await db.run('DELETE FROM Lotos');
  await db.run('DELETE FROM Checklists');
  await db.run('DELETE FROM Users');

  const areas = [
    { id: 'area_1', name: '공학1관 공사현장', isDanger: 0 },
    { id: 'area_2', name: '공학3관 변전실', isDanger: 1 }
  ];

  for (const area of areas) {
    await db.run('INSERT INTO Areas (id, name, isDanger) VALUES (?, ?, ?)', [area.id, area.name, area.isDanger]);
  }

  const lotos = [
    { id: 'loto_1', areaId: 'area_2', text: 'B-메인 가스벨브' },
    { id: 'loto_2', areaId: 'area_2', text: 'CNG 가스 벨브' }
  ];

  for (const loto of lotos) {
    await db.run('INSERT INTO Lotos (id, areaId, text) VALUES (?, ?, ?)', [loto.id, loto.areaId, loto.text]);
  }

  const checklists = [
    { id: 'chk_1', text: '안전 장비를 점검했나요?' },
    { id: 'chk_2', text: '안전줄을 잘 연결했나요?' }
  ];

  for (const chk of checklists) {
    await db.run('INSERT INTO Checklists (id, text) VALUES (?, ?)', [chk.id, chk.text]);
  }

  const users = [
    { employeeId: '12345', name: '홍길동' },
    { employeeId: '99999', name: '김안전' }
  ];

  for (const user of users) {
    await db.run('INSERT INTO Users (employeeId, name) VALUES (?, ?)', [user.employeeId, user.name]);
  }

  console.log('Database seeded successfully.');
  await db.close();
}

seed().catch(console.error);
