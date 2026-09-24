import { describe, it, expect } from 'vitest';
import {
  createIssueSchema,
  loginSchema,
  registerSchema,
} from '@/utils/schemas';

describe('createIssueSchema', () => {
  const valid = {
    projectKey: 'WF',
    type: 'TASK',
    summary: 'Implement login page',
    priority: 'MEDIUM',
  };

  it('accepts a minimal valid payload', () => {
    const result = createIssueSchema.safeParse(valid);
    expect(result.success).toBe(true);
  });

  it('requires a project', () => {
    const result = createIssueSchema.safeParse({ ...valid, projectKey: '' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.path.includes('projectKey'))).toBe(true);
    }
  });

  it('rejects a summary shorter than 3 characters', () => {
    const result = createIssueSchema.safeParse({ ...valid, summary: 'hi' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.path.includes('summary'))).toBe(true);
    }
  });

  it('rejects an invalid issue type', () => {
    const result = createIssueSchema.safeParse({ ...valid, type: 'FEATURE' });
    expect(result.success).toBe(false);
  });

  it('coerces story points from a numeric string', () => {
    const result = createIssueSchema.safeParse({ ...valid, storyPoints: '5' });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.storyPoints).toBe(5);
    }
  });

  it('allows an empty story points value', () => {
    const result = createIssueSchema.safeParse({ ...valid, storyPoints: '' });
    expect(result.success).toBe(true);
  });

  it('accepts labels and components arrays', () => {
    const result = createIssueSchema.safeParse({
      ...valid,
      labels: ['a', 'b'],
      components: ['c'],
    });
    expect(result.success).toBe(true);
  });
});

describe('loginSchema', () => {
  it('fails on empty input', () => {
    expect(loginSchema.safeParse({ usernameOrEmail: '', password: '' }).success).toBe(false);
  });

  it('passes with values', () => {
    expect(
      loginSchema.safeParse({ usernameOrEmail: 'ada', password: 'x' }).success,
    ).toBe(true);
  });
});

describe('registerSchema', () => {
  const base = {
    fullName: 'Ada Lovelace',
    username: 'ada',
    email: 'ada@company.com',
    password: 'secret123',
    confirmPassword: 'secret123',
  };

  it('accepts a valid registration', () => {
    expect(registerSchema.safeParse(base).success).toBe(true);
  });

  it('rejects mismatched passwords', () => {
    const result = registerSchema.safeParse({ ...base, confirmPassword: 'different1' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((i) => i.path.includes('confirmPassword'))).toBe(true);
    }
  });

  it('rejects a weak password without a number', () => {
    const result = registerSchema.safeParse({
      ...base,
      password: 'onlyletters',
      confirmPassword: 'onlyletters',
    });
    expect(result.success).toBe(false);
  });
});
