export interface JwtPayload {
  sub: string;
  householdId: string;
  role: 'owner' | 'member';
}

export interface AuthenticatedUser {
  id: string;
  email: string;
  displayName: string;
  householdId: string;
  role: 'owner' | 'member';
}
