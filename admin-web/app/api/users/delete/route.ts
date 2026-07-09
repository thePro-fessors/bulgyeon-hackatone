import { NextResponse } from 'next/server';
import { getDb } from '@/lib/db';

export async function POST(request: Request) {
  try {
    const { employeeId } = await request.json();
    if (!employeeId) return NextResponse.json({ success: false, message: 'Employee ID is required' }, { status: 400 });
    const db = await getDb();
    await db.run('DELETE FROM Users WHERE employeeId = ?', [employeeId]);
    return NextResponse.json({ success: true, message: 'User deleted successfully' });
  } catch (error: any) {
    return NextResponse.json({ success: false, message: error.message }, { status: 500 });
  }
}
