import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'secondary'

const styles: Record<Variant, string> = {
  primary: 'bg-sky text-mill shadow-[0_8px_20px_rgba(73,184,232,0.2)] hover:-translate-y-0.5 hover:bg-[#61c7f1] hover:shadow-[0_10px_25px_rgba(73,184,232,0.28)] disabled:bg-sky/60',
  secondary: 'border border-white/20 bg-white text-mill shadow-sm hover:-translate-y-0.5 hover:border-sky hover:bg-[#f0faff] disabled:text-ink-muted',
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
}

export const Button = ({ variant = 'primary', className = '', children, ...props }: ButtonProps) => (
  <button
    {...props}
    className={`rounded-full px-5 py-3 text-base font-bold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky disabled:cursor-not-allowed ${styles[variant]} ${className}`}
  >
    {children}
    {variant === 'primary' && <span aria-hidden="true" className="ml-2 inline-block">→</span>}
  </button>
)
