import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { DevLoginForm } from './DevLoginForm'

describe('DevLoginForm', () => {
  it('asks for both fields before calling sign-in', async () => {
    const onSignIn = vi.fn()
    render(<DevLoginForm onSignIn={onSignIn} />)

    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Enter your email and password.')
    expect(onSignIn).not.toHaveBeenCalled()
  })

  it('passes the credentials through and shows a failure message', async () => {
    const onSignIn = vi.fn().mockRejectedValue(new Error('That email and password do not match.'))
    render(<DevLoginForm onSignIn={onSignIn} />)

    await userEvent.type(screen.getByLabelText('Email'), 'client@acme.local')
    await userEvent.type(screen.getByLabelText('Password'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('That email and password do not match.')
    expect(onSignIn).toHaveBeenCalledWith({ email: 'client@acme.local', password: 'wrong' })
  })
})
