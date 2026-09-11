#!/usr/bin/env node
/**
 * describe-image.mjs — ask a vision model (Gemini free tier) what an image shows.
 *
 * Used to verify UI screenshots during development without booting the app: it
 * returns the visible layout, elements, headings and any text exactly as shown.
 *
 * Usage:
 *   node scripts/describe-image.mjs <image> [extra prompt]
 *
 * Env:
 *   GEMINI_API_KEY   (required) free key from https://aistudio.google.com/apikey
 *   GEMINI_MODEL     (optional, default gemini-3.6-flash)
 *   GEMINI_BASE_URL  (optional, default https://generativelanguage.googleapis.com)
 */
import { readFile } from 'node:fs/promises'
import { basename, extname } from 'node:path'

const MIME = {
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.gif': 'image/gif',
}

const DEFAULT_PROMPT =
  'Describe this screenshot in detail: the overall layout, every visible UI element, ' +
  'headings, buttons and numbers, and any text exactly as shown.'

const key = process.env.GEMINI_API_KEY
if (!key) {
  console.error('Set GEMINI_API_KEY first (free tier: https://aistudio.google.com/apikey)')
  process.exit(1)
}

const imagePath = process.argv[2]
if (!imagePath) {
  console.error('Usage: node scripts/describe-image.mjs <image> [extra prompt]')
  process.exit(1)
}

const mime = MIME[extname(imagePath).toLowerCase()]
if (!mime) {
  console.error(`Unsupported image type: ${extname(imagePath) || '(none)'} (need png/jpg/webp/gif)`)
  process.exit(1)
}

const data = (await readFile(imagePath)).toString('base64')
const prompt = process.argv.slice(3).join(' ') || DEFAULT_PROMPT

const model = process.env.GEMINI_MODEL || 'gemini-3.6-flash'
const base = (process.env.GEMINI_BASE_URL || 'https://generativelanguage.googleapis.com').replace(/\/$/, '')
const url = `${base}/v1beta/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(key)}`

const body = {
  contents: [
    {
      role: 'user',
      parts: [{ text: prompt }, { inline_data: { mime_type: mime, data } }],
    },
  ],
}

const maxAttempts = 5
let response
for (let attempt = 1; attempt <= maxAttempts; attempt++) {
  response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  const retryable = response.status === 429 || response.status === 500 || response.status === 503
  if (!retryable || attempt === maxAttempts) break
  const wait = 2 ** attempt * 1000
  console.error(`retry ${attempt}/${maxAttempts - 1} after ${response.status} — waiting ${wait / 1000}s`)
  await new Promise((resolve) => setTimeout(resolve, wait))
}

if (!response.ok) {
  console.error(`Gemini error ${response.status}: ${await response.text()}`)
  process.exit(1)
}

const json = await response.json()
const text =
  json?.candidates?.[0]?.content?.parts
    ?.map((part) => part.text)
    .join('') ?? '(no text returned — check the prompt or model)'

console.log(`\n${basename(imagePath)}\n${'─'.repeat(Math.min(60, basename(imagePath).length))}\n${text}\n`)