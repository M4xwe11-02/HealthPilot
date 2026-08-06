import { request } from './request';

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  isAdmin?: boolean;
  email?: string | null;
}

export interface AuthResponse {
  token: string;
  user: CurrentUser;
}

export interface LoginPayload {
  username: string;
  password: string;
}

export interface RegisterPayload extends LoginPayload {
  displayName?: string;
  email?: string;
  code?: string;
}

export interface EmailLoginPayload {
  email: string;
  code: string;
}

export const authApi = {
  login(payload: LoginPayload): Promise<AuthResponse> {
    return request.post<AuthResponse>('/api/auth/login', payload);
  },

  register(payload: RegisterPayload): Promise<AuthResponse> {
    return request.post<AuthResponse>('/api/auth/register', payload);
  },

  sendEmailCode(email: string): Promise<void> {
    return request.post<void>('/api/auth/email/code', {email});
  },

  loginWithEmail(payload: EmailLoginPayload): Promise<AuthResponse> {
    return request.post<AuthResponse>('/api/auth/email/login', payload);
  },

  bindEmail(payload: EmailLoginPayload): Promise<CurrentUser> {
    return request.post<CurrentUser>('/api/auth/email/bind', payload);
  },

  me(): Promise<CurrentUser> {
    return request.get<CurrentUser>('/api/auth/me');
  },

  logout(): Promise<void> {
    return request.post<void>('/api/auth/logout');
  },
};
