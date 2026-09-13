import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createCharacter, deleteCharacter, generateCharacter, listMyCharacters, sheetToDraft } from '../lib/characters'

export default function CharactersPage() {
  const navigate = useNavigate()
  const [characters, setCharacters] = useState([])
  const [loadError, setLoadError] = useState('')
  const [generating, setGenerating] = useState(false)
  const [preview, setPreview] = useState(null)
  const [pendingDraft, setPendingDraft] = useState(null)
  const [actionError, setActionError] = useState('')
  const [saving, setSaving] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    listMyCharacters()
      .then(setCharacters)
      .catch(() => setLoadError('Could not load your characters'))
  }, [])

  async function reload() {
    const loaded = await listMyCharacters()
    setCharacters(loaded)
  }

  async function handleSurpriseMe() {
    setActionError('')
    setGenerating(true)
    try {
      const result = await generateCharacter({ startingLevel: 1, seed: 0 })
      if (!result.valid) {
        setActionError(result.violations?.join(' ') || 'Could not assemble a legal quick-build')
        setPreview(null)
        setPendingDraft(null)
        return
      }
      setPendingDraft(sheetToDraft(result.sheet))
      setPreview(result.sheet)
    } catch (error) {
      setActionError(error.message)
    } finally {
      setGenerating(false)
    }
  }

  async function handleSaveQuickBuild() {
    setActionError('')
    setSaving(true)
    try {
      await createCharacter(pendingDraft)
      setPreview(null)
      setPendingDraft(null)
      await reload()
    } catch (error) {
      setActionError(error.message)
    } finally {
      setSaving(false)
    }
  }

  function handleReviseInWizard() {
    navigate('/characters/new', { state: { draft: pendingDraft } })
  }

  async function handleDelete() {
    if (!deleteTarget) return
    setActionError('')
    setDeleting(true)
    try {
      await deleteCharacter(deleteTarget.id)
      setCharacters((current) => current.filter((character) => character.id !== deleteTarget.id))
      setDeleteTarget(null)
    } catch (error) {
      setActionError(error.message)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold">My characters</h1>
        <div className="flex items-center gap-2">
          <Link
            to="/characters/new"
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700"
          >
            Guided wizard
          </Link>
          <button
            type="button"
            onClick={handleSurpriseMe}
            disabled={generating}
            className="rounded-md bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
          >
            {generating ? 'Rolling…' : 'Create Random Character'}
          </button>
        </div>
      </div>

      {loadError && (
        <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {loadError}
        </p>
      )}
      {actionError && (
        <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {actionError}
        </p>
      )}

      {preview && (
        <section className="rounded-xl border border-emerald-300 bg-emerald-50 p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="font-semibold">Quick build: {preview.name}</h2>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleReviseInWizard}
                className="rounded-md border border-zinc-300 px-3 py-1 text-sm text-zinc-700 hover:bg-white"
              >
                Open in wizard
              </button>
              <button
                type="button"
                onClick={handleSaveQuickBuild}
                disabled={saving}
                className="rounded-md bg-zinc-900 px-3 py-1 text-sm font-medium text-white hover:bg-zinc-700 disabled:opacity-50"
              >
                {saving ? 'Saving…' : 'Save this character'}
              </button>
            </div>
          </div>
          <p className="mt-2 text-sm text-zinc-600">{preview.raceIndex} {preview.classIndex} · level {preview.level} · HP {preview.hitPoints} · AC {preview.armorClass}</p>
        </section>
      )}

      {characters.length === 0 && !loadError ? (
        <p className="text-sm text-zinc-500">
          No characters yet — use the wizard to build your first hero, or hit “Create Random Character”.
        </p>
      ) : (
        <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {characters.map((character) => (
            <li
              key={character.id}
              className="rounded-xl border border-zinc-200 bg-white p-4 shadow-sm hover:border-zinc-400"
            >
              <Link to={`/characters/${character.id}`} className="block">
                <div className="flex items-center justify-between">
                  <span className="font-semibold">{character.name}</span>
                  <span className="text-sm text-zinc-500">Level {character.level}</span>
                </div>
                <p className="mt-1 text-sm text-zinc-500">{character.raceIndex} {character.classIndex}{character.subclassIndex ? ` (${character.subclassIndex})` : ''} · {character.backgroundIndex}</p>
                <p className="mt-2 text-sm text-zinc-600">HP {character.hitPoints} · AC {character.armorClass}</p>
              </Link>
              <div className="mt-3 flex justify-end">
                <button
                  type="button"
                  onClick={() => setDeleteTarget(character)}
                  className="rounded-md border border-red-200 px-3 py-1 text-sm text-red-700 hover:bg-red-50"
                >
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {deleteTarget && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-confirm-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-900/50 p-4"
        >
          <div className="w-full max-w-sm rounded-xl bg-white p-6 shadow-xl">
            <h2 id="delete-confirm-title" className="font-semibold">Delete {deleteTarget.name}?</h2>
            <p className="mt-2 text-sm text-zinc-500">
              This removes the character (and its sheet) for good. This can't be undone.
            </p>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setDeleteTarget(null)}
                disabled={deleting}
                className="rounded-md border border-zinc-300 px-3 py-1 text-sm text-zinc-700 hover:bg-zinc-50 disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDelete}
                disabled={deleting}
                className="rounded-md bg-red-700 px-3 py-1 text-sm font-medium text-white hover:bg-red-600 disabled:opacity-50"
              >
                {deleting ? 'Deleting…' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}