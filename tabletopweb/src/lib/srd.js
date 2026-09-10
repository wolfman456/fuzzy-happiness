import { api } from './api'

function toQuery(params) {
  const entries = Object.entries(params ?? {}).filter(([, value]) => value !== '' && value !== undefined)
  if (entries.length === 0) return ''
  const qs = new URLSearchParams(entries).toString()
  return `?${qs}`
}

export function srdList(collection, params) {
  return api(`/api/srd/${collection}${toQuery(params)}`)
}

export function srdDetail(collection, index) {
  return api(`/api/srd/${collection}/${index}`)
}

export function srdSubresource(collection, index, subresource) {
  return api(`/api/srd/${collection}/${index}/${subresource}`)
}

/** Normalizes a list endpoint's `results` into plain {index, name} rows. */
export function srdRows(json) {
  return (json?.results ?? []).map((row) => ({ index: row.index, name: row.name }))
}

/** Class skills a character is offered to pick from (`proficiency_choices`). */
export function offeredClassSkills(classRecord) {
  const choices = classRecord?.proficiency_choices ?? []
  const skills = new Set()
  for (const choice of choices) {
    for (const option of choice?.from?.options ?? []) {
      const index = option?.item?.index
      if (typeof index === 'string' && index.startsWith('skill-')) skills.add(index)
    }
  }
  return skills
}

export function subclassLevel(classRecord) {
  return typeof classRecord?.subclass_level === 'number' ? classRecord.subclass_level : 1
}