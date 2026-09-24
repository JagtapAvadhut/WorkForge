import { useQuery } from '@tanstack/react-query';
import { projectApi } from '@/api/projectApi';
import { issueApi } from '@/api/issueApi';
import { sprintApi } from '@/api/sprintApi';
import { userApi } from '@/api/userApi';

export function useProjectsAll() {
  return useQuery({ queryKey: ['projects', 'all'], queryFn: () => projectApi.listAll() });
}

export function useProjectStatuses(projectKey?: string) {
  return useQuery({
    queryKey: ['project', projectKey, 'statuses'],
    queryFn: () => issueApi.statuses(projectKey!),
    enabled: Boolean(projectKey),
  });
}

export function useProjectLabels(projectKey?: string) {
  return useQuery({
    queryKey: ['project', projectKey, 'labels'],
    queryFn: () => issueApi.labels(projectKey!),
    enabled: Boolean(projectKey),
  });
}

export function useProjectComponents(projectKey?: string) {
  return useQuery({
    queryKey: ['project', projectKey, 'components'],
    queryFn: () => issueApi.components(projectKey!),
    enabled: Boolean(projectKey),
  });
}

export function useProjectSprints(projectKey?: string) {
  return useQuery({
    queryKey: ['project', projectKey, 'sprints'],
    queryFn: () => sprintApi.listByProject(projectKey!),
    enabled: Boolean(projectKey),
  });
}

export function useUserSearch(query: string, projectKey?: string) {
  return useQuery({
    queryKey: ['users', 'search', query, projectKey],
    queryFn: () => userApi.search(query, projectKey),
    enabled: query.length >= 1,
  });
}
