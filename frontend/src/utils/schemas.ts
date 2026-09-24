import { z } from 'zod';
import { ISSUE_PRIORITIES, ISSUE_TYPES } from './issueMeta';

export const loginSchema = z.object({
  usernameOrEmail: z.string().min(1, 'Username or email is required'),
  password: z.string().min(1, 'Password is required'),
});
export type LoginFormValues = z.infer<typeof loginSchema>;

export const registerSchema = z
  .object({
    fullName: z.string().min(2, 'Please enter your full name'),
    username: z
      .string()
      .min(3, 'Username must be at least 3 characters')
      .max(30, 'Username must be at most 30 characters')
      .regex(/^[a-zA-Z0-9_.-]+$/, 'Only letters, numbers, and . _ - are allowed'),
    email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .regex(/[A-Za-z]/, 'Include at least one letter')
      .regex(/[0-9]/, 'Include at least one number'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });
export type RegisterFormValues = z.infer<typeof registerSchema>;

export const createIssueSchema = z.object({
  projectKey: z.string().min(1, 'Select a project'),
  type: z.enum(ISSUE_TYPES as [string, ...string[]]),
  summary: z
    .string()
    .trim()
    .min(3, 'Summary must be at least 3 characters')
    .max(255, 'Summary must be at most 255 characters'),
  description: z.string().max(10_000, 'Description is too long').optional().or(z.literal('')),
  priority: z.enum(ISSUE_PRIORITIES as [string, ...string[]]),
  assigneeId: z.string().optional().or(z.literal('')),
  sprintId: z.string().optional().or(z.literal('')),
  labels: z.array(z.string()).optional(),
  components: z.array(z.string()).optional(),
  storyPoints: z
    .union([z.coerce.number().min(0, 'Must be ≥ 0').max(1000, 'Too large'), z.literal('')])
    .optional(),
  dueDate: z.string().optional().or(z.literal('')),
});
export type CreateIssueFormValues = z.infer<typeof createIssueSchema>;

export const createProjectSchema = z.object({
  key: z
    .string()
    .trim()
    .min(2, 'Key must be 2-10 uppercase letters')
    .max(10, 'Key must be 2-10 uppercase letters')
    .regex(/^[A-Z][A-Z0-9]+$/, 'Use uppercase letters/numbers, e.g. WF'),
  name: z.string().trim().min(2, 'Project name is required').max(80, 'Name is too long'),
  description: z.string().max(2000).optional().or(z.literal('')),
});
export type CreateProjectFormValues = z.infer<typeof createProjectSchema>;

export const createSprintSchema = z.object({
  name: z.string().trim().min(2, 'Sprint name is required').max(80),
  goal: z.string().max(500).optional().or(z.literal('')),
  startDate: z.string().optional().or(z.literal('')),
  endDate: z.string().optional().or(z.literal('')),
});
export type CreateSprintFormValues = z.infer<typeof createSprintSchema>;

export const commentSchema = z.object({
  body: z.string().trim().min(1, 'Comment cannot be empty').max(5000, 'Comment is too long'),
});
export type CommentFormValues = z.infer<typeof commentSchema>;
