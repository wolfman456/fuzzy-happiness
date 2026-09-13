export function ageInYears(dateOfBirth, today = new Date()) {
  const born = new Date(`${dateOfBirth}T00:00:00`)
  if (Number.isNaN(born.getTime())) return NaN
  let years = today.getFullYear() - born.getFullYear()
  const beforeBirthday =
    today.getMonth() < born.getMonth() ||
    (today.getMonth() === born.getMonth() && today.getDate() < born.getDate())
  if (beforeBirthday) years -= 1
  return years
}

export function isAdult(dateOfBirth, minimumAge = 13, today = new Date()) {
  return ageInYears(dateOfBirth, today) >= minimumAge
}