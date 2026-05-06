import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { UserRole } from '../../types';

interface RoleGuardProps {
  children: React.ReactNode;
  requiredRoles: UserRole | UserRole[];
  fallback?: React.ReactNode;
}

export const RoleGuard: React.FC<RoleGuardProps> = ({ 
  children, 
  requiredRoles, 
  fallback = null 
}) => {
  const { hasAnyRole } = useAuth();
  const roles = Array.isArray(requiredRoles) ? requiredRoles : [requiredRoles];
  
  if (hasAnyRole(roles)) {
    return <>{children}</>;
  }
  
  return <>{fallback}</>;
};