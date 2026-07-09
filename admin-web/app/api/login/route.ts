import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  const { employeeId } = await request.json();
  const db = await getDb();
  const user = await db.get('SELECT * FROM Users WHERE employeeId = ?', [employeeId]);
  
  if (user) {
    return NextResponse.json({ success: true, user });
  } else {
    return NextResponse.json({ success: false, message: 'User not found' }, { status: 401 });
  }
}
