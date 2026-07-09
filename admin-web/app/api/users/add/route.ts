import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  try {
    const { employeeId, name } = await request.json();
    if (!employeeId || !name) {
      return NextResponse.json({ success: false, message: 'Employee ID and Name are required' }, { status: 400 });
    }
    const db = await getDb();
    await db.run('INSERT INTO Users (employeeId, name) VALUES (?, ?)', [employeeId, name]);
    return NextResponse.json({ success: true, message: 'User added successfully' });
  } catch (error: any) {
    return NextResponse.json({ success: false, message: error.message }, { status: 500 });
  }
}
