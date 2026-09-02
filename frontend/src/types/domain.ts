export type Role = 'PARENT' | 'MEMBER'
export type UserStatus = 'ACTIVE' | 'INACTIVE'
export type CategoryType = 'INCOME' | 'EXPENSE'
export type CategoryScope = 'SYSTEM' | 'CUSTOM'
export type CategoryStatus = 'ACTIVE' | 'INACTIVE'
export type LedgerType = CategoryType

export interface User {
  id: number
  username: string
  displayName: string
  memberNo: string | null
  role: Role | null
  status: UserStatus
  hasHousehold: boolean
  household: { id: number; name: string } | null
}

export interface Household {
  id: number
  name: string
  inviteCode: string | null
  currentMemberId: number
  currentMemberNo: string
  currentRole: Role
}

export interface Member {
  id: number
  memberNo: string
  displayName: string
  role: Role
  status: UserStatus
  joinedAt: string
}

export interface Category {
  id: number
  scope: CategoryScope
  type: CategoryType
  name: string
  status: CategoryStatus
}

export type Money = string | number

export interface Entry {
  id: number
  memberId: number
  memberName: string
  memberNo: string
  type: LedgerType
  categoryId: number
  categoryName: string
  amount: Money
  occurredOn: string
  note: string | null
  createdAt: string
}

export interface EntryPage {
  items: Entry[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface Dashboard {
  from: string
  to: string
  totals: { income: Money; expense: Money; balance: Money }
  trend: { month: string; income: Money; expense: Money }[]
  composition: { categoryId: number; categoryName: string; type: CategoryType; amount: Money }[]
  members: { memberId: number; memberNo: string; memberName: string; income: Money; expense: Money; balance: Money }[]
  recentEntries: Entry[]
}
