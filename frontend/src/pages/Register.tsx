import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { useToast } from '@/components/ui/Toast';
import { useRegister } from '@/hooks/useAuth';
import { registerSchema, type RegisterFormValues } from '@/utils/schemas';

export default function Register() {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema) });
  const registerMutation = useRegister();
  const navigate = useNavigate();
  const toast = useToast();

  const onSubmit = (values: RegisterFormValues) => {
    registerMutation.mutate(
      {
        fullName: values.fullName,
        username: values.username,
        email: values.email,
        password: values.password,
      },
      {
        onSuccess: () => {
          toast.success('Account created', 'Welcome to WorkForge');
          navigate('/dashboard', { replace: true });
        },
        onError: (err: unknown) => {
          toast.error('Registration failed', err instanceof Error ? err.message : undefined);
        },
      },
    );
  };

  return (
    <div>
      <h1 className="text-lg font-semibold text-content">Create your account</h1>
      <p className="mt-1 text-sm text-content-muted">Start tracking work in minutes.</p>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4" noValidate>
        <Input
          label="Full name"
          autoComplete="name"
          placeholder="Ada Lovelace"
          error={errors.fullName?.message}
          {...register('fullName')}
        />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Input
            label="Username"
            autoComplete="username"
            placeholder="ada"
            error={errors.username?.message}
            {...register('username')}
          />
          <Input
            label="Email"
            type="email"
            autoComplete="email"
            placeholder="ada@company.com"
            error={errors.email?.message}
            {...register('email')}
          />
        </div>
        <Input
          label="Password"
          type="password"
          autoComplete="new-password"
          placeholder="At least 8 characters"
          error={errors.password?.message}
          {...register('password')}
        />
        <Input
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          placeholder="Re-enter your password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />

        <Button type="submit" size="lg" className="w-full" loading={registerMutation.isPending}>
          Create account
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-content-muted">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-accent hover:underline">
          Sign in
        </Link>
      </p>
    </div>
  );
}
