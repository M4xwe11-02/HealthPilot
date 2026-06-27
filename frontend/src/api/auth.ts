import { request } from './request';

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  isAdmin?: boolean;
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
}

export const authApi = {
  login(payload: LoginPayload): Promise<AuthResponse> {
    return request.post<AuthResponse>('/api/auth/login', payload);
  },

  register(payload: RegisterPayload): Promise<AuthResponse> {
    return request.post<AuthResponse>('/api/auth/register', payload);
  },

  me(): Promise<CurrentUser> {
    return request.get<CurrentUser>('/api/auth/me');
  },

  logout(): Promise<void> {
    return request.post<void>('/api/auth/logout');
  },
};
