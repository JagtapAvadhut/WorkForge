import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Eye, EyeOff } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { useToast } from '@/components/ui/Toast';
import { useLogin } from '@/hooks/useAuth';
import { loginSchema, type LoginFormValues } from '@/utils/schemas';

export default function Login() {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) });
  const [showPassword, setShowPassword] = useState(false);
  const login = useLogin();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const toast = useToast();

  const onSubmit = (values: LoginFormValues) => {
    login.mutate(values, {
      onSuccess: () => {
        toast.success('Welcome back');
        navigate(params.get('redirect') || '/dashboard', { replace: true });
      },
      onError: (err: unknown) => {
        toast.error('Sign in failed', err instanceof Error ? err.message : 'Check your credentials');
      },
    });
  };

  return (
    <div>
      <h1 className="text-lg font-semibold text-content">Sign in to your account</h1>
      <p className="mt-1 text-sm text-content-muted">Enter your credentials to continue.</p>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4" noValidate>
        <Input
          label="Username or email"
          autoComplete="username"
          placeholder="you@company.com"
          error={errors.usernameOrEmail?.message}
          {...register('usernameOrEmail')}
        />
        <div className="relative">
          <Input
            label="Password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            placeholder="••••••••"
            error={errors.password?.message}
            {...register('password')}
          />
          <button
            type="button"
            onClick={() => setShowPassword((s) => !s)}
            className="absolute right-3 top-[34px] text-content-subtle hover:text-content"
            aria-label={showPassword ? 'Hide password' : 'Show password'}
            tabIndex={-1}
          >
            {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
          </button>
        </div>

        <Button type="submit" size="lg" className="w-full" loading={login.isPending}>
          Sign in
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-content-muted">
        Don&apos;t have an account?{' '}
        <Link to="/register" className="font-medium text-accent hover:underline">
          Create one
        </Link>
      </p>
    </div>
  );
}
