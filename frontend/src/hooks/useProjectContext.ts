import { useOutletContext } from 'react-router-dom';
import type { Project } from '@/types';

export interface ProjectOutletContext {
  project: Project;
}

export function useProjectContext(): ProjectOutletContext {
  return useOutletContext<ProjectOutletContext>();
}
