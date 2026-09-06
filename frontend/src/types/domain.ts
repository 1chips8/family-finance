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
  comparison?: { income: Money | null; expense: Money | null; balance: Money | null; incomeChangeRate?: Money | null; expenseChangeRate?: Money | null; balanceChangeRate?: Money | null; previous?: { income: Money; expense: Money; balance: Money } | null }
  savingsRate?: number | null
  budget?: BudgetOverview | null
  anomalies?: CategoryAnomaly[]
}

export type BudgetStatus = 'NORMAL' | 'WARNING' | 'OVER'
export interface BudgetItem {
  id?: number
  categoryId?: number | null
  categoryName?: string | null
  budget: Money
  spent: Money
  remaining: Money
  usageRate: number
  status: BudgetStatus
}
export interface BudgetOverview {
  month: string
  total: BudgetItem | null
  categories: BudgetItem[]
}
export interface CategoryAnomaly {
  categoryId: number
  categoryName: string
  currentAmount: Money
  baselineAmount: Money
  ratio: number
  message?: string
}
export interface RecurringTemplate {
  id: number
  memberId: number
  memberNo?: string
  memberName?: string
  type: LedgerType
  categoryId: number
  categoryName?: string
  amount: Money
  dayOfMonth: number
  note: string | null
  active: boolean
  createdAt?: string
  updatedAt?: string
}
export interface RecurringGenerationResult {
  month: string
  created: number
  alreadyGenerated: number
  skipped: number
  skippedReasons?: string[]
  skipReasons?: Array<{ templateId?: number; reason: string }>
}
export interface AuditLog {
  id: number
  actorId?: number
  actorName?: string
  actorMemberNo?: string
  action: string
  objectType: string
  objectId?: number | null
  summary: string
  createdAt: string
}
export interface AuditLogPage {
  items: AuditLog[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}
