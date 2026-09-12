import { SetMetadata } from '@nestjs/common';

export const IS_PUBLIC_KEY = 'isPublic';

/**
 * Marks a route as not requiring authentication. Used by the global
 * JwtAuthGuard (see AppModule) to skip token verification.
 */
export const Public = () => SetMetadata(IS_PUBLIC_KEY, true);
