import { useNavigate } from 'react-router-dom';
import { Compass } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export default function NotFound() {
  const navigate = useNavigate();
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center text-center">
      <span className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-accent/10 text-accent">
        <Compass size={30} />
      </span>
      <p className="font-mono text-sm text-content-subtle">404</p>
      <h1 className="mt-1 text-2xl font-semibold text-content">Page not found</h1>
      <p className="mt-2 max-w-sm text-sm text-content-muted">
        The page you're looking for doesn't exist or may have been moved.
      </p>
      <Button className="mt-6" onClick={() => navigate('/dashboard')}>
        Back to dashboard
      </Button>
    </div>
  );
}
