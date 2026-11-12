const WEEKDAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
const MONTH_FORMAT = new Intl.DateTimeFormat('en', { month: 'long', year: 'numeric' });

function dateKey(year, monthIndex, day) {
  return `${String(year).padStart(4, '0')}-${String(monthIndex + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

function validIsoDate(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const [year, month, day] = value.split('-').map(Number);
  const parsed = new Date(year, month - 1, day);
  return parsed.getFullYear() === year && parsed.getMonth() === month - 1 && parsed.getDate() === day;
}

export function buildLeaveCalendar(monthDate, requests = [], today = new Date()) {
  if (!(monthDate instanceof Date) || Number.isNaN(monthDate.getTime())) {
    throw new TypeError('A valid month date is required.');
  }
  if (!Array.isArray(requests)) throw new TypeError('Requests must be an array.');

  const year = monthDate.getFullYear();
  const monthIndex = monthDate.getMonth();
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
  const mondayOffset = (new Date(year, monthIndex, 1).getDay() + 6) % 7;
  const todayKey = today instanceof Date && !Number.isNaN(today.getTime())
    ? dateKey(today.getFullYear(), today.getMonth(), today.getDate()) : '';

  const validRequests = requests.filter(request => request
    && validIsoDate(request.startDate)
    && validIsoDate(request.endDate)
    && request.startDate <= request.endDate);

  const cells = Array.from({ length: 42 }, (_, index) => {
    const day = index - mondayOffset + 1;
    const inMonth = day >= 1 && day <= daysInMonth;
    const key = inMonth ? dateKey(year, monthIndex, day) : '';
    const dayRequests = inMonth ? validRequests.filter(request => request.startDate <= key && key <= request.endDate) : [];
    return {
      date: key,
      day: inMonth ? day : null,
      inMonth,
      isToday: Boolean(key && key === todayKey),
      requests: dayRequests
    };
  });

  return {
    title: MONTH_FORMAT.format(new Date(year, monthIndex, 1)),
    weekdays: WEEKDAYS.slice(),
    weeks: Array.from({ length: 6 }, (_, index) => cells.slice(index * 7, index * 7 + 7))
  };
}
