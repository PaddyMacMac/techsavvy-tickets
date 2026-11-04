import type { PublicClientApplication } from '@azure/msal-browser'
import type { AuthStrategy } from './authStrategy'

export interface EntraConfig {
  tenantId: string
  clientId: string
}

/** Production sign-in: Microsoft Entra ID, authorisation-code flow with PKCE, by redirect. */
export const createMsalStrategy = ({ tenantId, clientId }: EntraConfig): AuthStrategy => {
  const scopes = [`api://${clientId}/tickets.access`]

  let clientPromise: Promise<PublicClientApplication> | undefined
  const getClient = () =>
    (clientPromise ??= import('@azure/msal-browser').then(async ({ PublicClientApplication }) => {
      const msal = new PublicClientApplication({
        auth: {
          clientId,
          authority: `https://login.microsoftonline.com/${tenantId}`,
          redirectUri: window.location.origin,
        },
        cache: { cacheLocation: 'sessionStorage' },
      })
      await msal.initialize()
      const result = await msal.handleRedirectPromise()
      const account = result?.account ?? msal.getAllAccounts()[0] ?? null
      if (account) msal.setActiveAccount(account)
      return msal
    }))

  const initialise = async () => (await getClient()).getActiveAccount() !== null

  const getAccessToken = async () => {
    const [{ InteractionRequiredAuthError }, msal] = await Promise.all([import('@azure/msal-browser'), getClient()])
    const account = msal.getActiveAccount()
    if (!account) return null
    try {
      return (await msal.acquireTokenSilent({ scopes, account })).accessToken
    } catch (error) {
      if (error instanceof InteractionRequiredAuthError) {
        await msal.acquireTokenRedirect({ scopes, account })
        return null
      }
      throw error
    }
  }

  return {
    kind: 'entra',
    initialise,
    signIn: async () => (await getClient()).loginRedirect({ scopes }),
    signOut: async () => {
      const msal = await getClient()
      await msal.logoutRedirect({
        account: msal.getActiveAccount() ?? undefined,
        postLogoutRedirectUri: window.location.origin,
      })
    },
    getAccessToken,
  }
}
