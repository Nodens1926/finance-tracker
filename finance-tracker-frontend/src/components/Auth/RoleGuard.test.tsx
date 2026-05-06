import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { RoleGuard } from './RoleGuard';
import { AuthContext } from '../../context/AuthContext';

const mockUseAuth = vi.fn();

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
  AuthContext: { Provider: ({ children }: any) => children },
}));

describe('RoleGuard', () => {
  it('renders children when user has required role', () => {
    mockUseAuth.mockReturnValue({
      hasAnyRole: (roles: string[]) => roles.includes('ROLE_ADMIN'),
    });

    render(
      <RoleGuard requiredRoles="ROLE_ADMIN">
        <div>Admin Content</div>
      </RoleGuard>
    );

    expect(screen.getByText('Admin Content')).toBeInTheDocument();
  });

  it('renders children when user has one of required roles', () => {
    mockUseAuth.mockReturnValue({
      hasAnyRole: (roles: string[]) => roles.includes('ROLE_MANAGER'),
    });

    render(
      <RoleGuard requiredRoles={['ROLE_ADMIN', 'ROLE_MANAGER']}>
        <div>Manager Content</div>
      </RoleGuard>
    );

    expect(screen.getByText('Manager Content')).toBeInTheDocument();
  });

  it('renders fallback when user lacks required role', () => {
    mockUseAuth.mockReturnValue({
      hasAnyRole: () => false,
    });

    render(
      <RoleGuard requiredRoles="ROLE_ADMIN" fallback={<div>Access Denied</div>}>
        <div>Admin Content</div>
      </RoleGuard>
    );

    expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
    expect(screen.getByText('Access Denied')).toBeInTheDocument();
  });

  it('renders nothing when user lacks role and no fallback', () => {
    mockUseAuth.mockReturnValue({
      hasAnyRole: () => false,
    });

    render(
      <RoleGuard requiredRoles="ROLE_ADMIN">
        <div>Admin Content</div>
      </RoleGuard>
    );

    expect(screen.queryByText('Admin Content')).not.toBeInTheDocument();
  });
});