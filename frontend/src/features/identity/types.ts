export type Role = 'ENGINEER' | 'CLIENT'

export interface Me {
  subject: string
  name: string
  email: string
  roles: Role[]
}
