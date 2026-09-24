export type Role = 'ADMIN' | 'PROJECT_LEAD' | 'MEMBER' | 'VIEWER';

export interface User {
  id: string;
  email: string;
  username: string;
  fullName: string;
  displayName?: string;
  avatarUrl?: string | null;
  roles: Role[];
  active: boolean;
  createdAt: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
}

export interface AuthResult {
  user: User;
  tokens: AuthTokens;
}

export type ProjectRole = 'LEAD' | 'MEMBER' | 'VIEWER';

export interface Project {
  id: string;
  key: string;
  name: string;
  description?: string | null;
  lead?: User | null;
  memberCount?: number;
  issueCount?: number;
  openIssueCount?: number;
  avatarColor?: string | null;
  createdAt: string;
}

export interface ProjectMember {
  user: User;
  role: ProjectRole;
  joinedAt: string;
}

export type IssueType = 'STORY' | 'TASK' | 'BUG' | 'EPIC' | 'SUBTASK';
export type IssuePriority = 'HIGHEST' | 'HIGH' | 'MEDIUM' | 'LOW' | 'LOWEST';
export type IssueStatusCategory = 'TODO' | 'IN_PROGRESS' | 'DONE';

export interface IssueStatus {
  id: string;
  name: string;
  category: IssueStatusCategory;
  color?: string | null;
  order?: number;
}

export interface Label {
  id: string;
  name: string;
  color?: string | null;
}

export interface Component {
  id: string;
  name: string;
}

export interface Issue {
  id: string;
  key: string;
  projectKey: string;
  projectName?: string;
  type: IssueType;
  summary: string;
  description?: string | null;
  status: IssueStatus;
  priority: IssuePriority;
  assignee?: User | null;
  reporter?: User | null;
  sprintId?: string | null;
  sprintName?: string | null;
  labels: Label[];
  components: Component[];
  storyPoints?: number | null;
  dueDate?: string | null;
  rank?: string;
  createdAt: string;
  updatedAt: string;
  commentCount?: number;
}

export type SprintState = 'FUTURE' | 'ACTIVE' | 'COMPLETED';

export interface Sprint {
  id: string;
  name: string;
  goal?: string | null;
  state: SprintState;
  startDate?: string | null;
  endDate?: string | null;
  projectKey: string;
  issueCount?: number;
  completedPoints?: number;
  totalPoints?: number;
}

export interface Comment {
  id: string;
  issueKey: string;
  author: User;
  body: string;
  createdAt: string;
  updatedAt?: string | null;
  edited?: boolean;
}

export type ActivityType =
  | 'CREATED'
  | 'STATUS_CHANGED'
  | 'ASSIGNED'
  | 'PRIORITY_CHANGED'
  | 'COMMENTED'
  | 'UPDATED'
  | 'SPRINT_CHANGED';

export interface ActivityEntry {
  id: string;
  type: ActivityType;
  actor: User;
  field?: string | null;
  from?: string | null;
  to?: string | null;
  createdAt: string;
}

export type NotificationType =
  | 'ASSIGNED'
  | 'MENTIONED'
  | 'COMMENTED'
  | 'STATUS_CHANGED'
  | 'DUE_SOON';

export interface AppNotification {
  id: string;
  type: NotificationType;
  title: string;
  body?: string | null;
  issueKey?: string | null;
  read: boolean;
  createdAt: string;
}

export interface BoardColumnDef {
  id: string;
  name: string;
  category: IssueStatusCategory;
  statusIds: string[];
  wipLimit?: number | null;
}

export interface Board {
  id: string;
  name: string;
  projectKey: string;
  columns: BoardColumnDef[];
}

export interface SavedFilter {
  id: string;
  name: string;
  description?: string | null;
  jql: string;
  owner?: User | null;
  shared: boolean;
  createdAt: string;
}

export interface DashboardStats {
  openIssues: number;
  assignedToMe: number;
  reportedByMe: number;
  dueSoon: number;
  statusDistribution: { category: IssueStatusCategory; label: string; count: number }[];
  priorityDistribution: { priority: IssuePriority; count: number }[];
}

export interface SearchResults {
  issues: Issue[];
  projects: Project[];
  users: User[];
}
