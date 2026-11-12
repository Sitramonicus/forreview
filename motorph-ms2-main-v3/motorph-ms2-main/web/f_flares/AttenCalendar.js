const WEEKDAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
const MONTH_FORMAT = new Intl.DateTimeFormat('en', { month: 'long', year: 'numeric' });

function dateKey(year, monthIndex, day) {
  return `${String(year).padStart(4, '0')}-${String(monthIndex + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

function isIsoDate(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const [year, month, day] = value.split('-').map(Number);
  const parsed = new Date(year, month - 1, day);
  return parsed.getFullYear() === year && parsed.getMonth() === month - 1 && parsed.getDate() === day;
}

export function buildAttendanceCalendar(monthDate, records = []) {
  if (!(monthDate instanceof Date) || Number.isNaN(monthDate.getTime())) {
    throw new TypeError('A valid month date is required.');
  }
  if (!Array.isArray(records)) throw new TypeError('Attendance records must be an array.');

  const year = monthDate.getFullYear();
  const monthIndex = monthDate.getMonth();
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
  const mondayOffset = (new Date(year, monthIndex, 1).getDay() + 6) % 7;
  const byDate = new Map();
  records.filter(record => record && isIsoDate(record.date) && record.date.startsWith(`${year}-${String(monthIndex + 1).padStart(2, '0')}-`))
    .forEach(record => {
      if (!byDate.has(record.date)) byDate.set(record.date, []);
      byDate.get(record.date).push(record);
    });

  const cells = Array.from({ length: 42 }, (_, index) => {
    const day = index - mondayOffset + 1;
    const inMonth = day >= 1 && day <= daysInMonth;
    const date = inMonth ? dateKey(year, monthIndex, day) : '';
    return {
      date,
      day: inMonth ? day : null,
      inMonth,
      records: inMonth ? (byDate.get(date) || []).slice() : []
    };
  });

  return {
    title: MONTH_FORMAT.format(new Date(year, monthIndex, 1)),
    weekdays: WEEKDAYS.slice(),
    weeks: Array.from({ length: 6 }, (_, index) => cells.slice(index * 7, index * 7 + 7))
  };
}
