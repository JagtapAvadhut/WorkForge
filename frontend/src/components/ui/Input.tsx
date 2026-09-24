import { forwardRef, type InputHTMLAttributes, type TextareaHTMLAttributes, useId } from 'react';
import { cn } from '@/utils/cn';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, hint, id, ...props }, ref) => {
    const autoId = useId();
    const inputId = id ?? autoId;
    return (
      <div className="w-full">
        {label ? (
          <label htmlFor={inputId} className="wf-label">
            {label}
          </label>
        ) : null}
        <input
          id={inputId}
          ref={ref}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined}
          className={cn('wf-input', error && 'border-danger focus:border-danger focus:ring-danger/30', className)}
          {...props}
        />
        {error ? (
          <p id={`${inputId}-error`} className="mt-1 text-xs font-medium text-danger">
            {error}
          </p>
        ) : hint ? (
          <p id={`${inputId}-hint`} className="mt-1 text-xs text-content-subtle">
            {hint}
          </p>
        ) : null}
      </div>
    );
  },
);
Input.displayName = 'Input';

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, label, error, hint, id, rows = 4, ...props }, ref) => {
    const autoId = useId();
    const inputId = id ?? autoId;
    return (
      <div className="w-full">
        {label ? (
          <label htmlFor={inputId} className="wf-label">
            {label}
          </label>
        ) : null}
        <textarea
          id={inputId}
          ref={ref}
          rows={rows}
          aria-invalid={Boolean(error)}
          className={cn('wf-input resize-y', error && 'border-danger focus:border-danger focus:ring-danger/30', className)}
          {...props}
        />
        {error ? (
          <p className="mt-1 text-xs font-medium text-danger">{error}</p>
        ) : hint ? (
          <p className="mt-1 text-xs text-content-subtle">{hint}</p>
        ) : null}
      </div>
    );
  },
);
Textarea.displayName = 'Textarea';
