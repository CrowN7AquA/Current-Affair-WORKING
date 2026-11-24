export const auth = {};
export const firebaseReady = true;
export const googleProvider = 'google', appleProvider = 'apple';
export const browserLocalPersistence = 'local', browserSessionPersistence = 'session';
let storage = sessionStorage;
const token = () => sessionStorage.getItem('nowline_token') || localStorage.getItem('nowline_token');
export async function api(path, data = {}) {
  const response = await fetch('/api/' + path, {method:'POST', headers:{'X-Session':token() || '', 'Content-Type':'application/x-www-form-urlencoded'}, body:new URLSearchParams(data)});
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || 'Request failed');
  return result;
}
export async function setPersistence(_, mode) { storage = mode === 'local' ? localStorage : sessionStorage; }
async function authenticate(path, data) {
  const result = await api(path,data);
  localStorage.removeItem('nowline_token'); sessionStorage.removeItem('nowline_token');
  storage.setItem('nowline_token',result.token); return {user:result.user};
}
export const signInWithEmailAndPassword = (_,email,password) => authenticate('login',{email,password});
export const createUserWithEmailAndPassword = (_,email,password,name) => authenticate('register',{email,password,name});
export async function signOut() { try {await api('logout');} finally {localStorage.removeItem('nowline_token');sessionStorage.removeItem('nowline_token');} }
export function onAuthStateChanged(_,callback) { let active=true; (token()?api('me'):Promise.resolve(null)).then(u=>{if(active)callback(u)}).catch(()=>{if(active)callback(null)}); return ()=>{active=false}; }
export async function saveUser() {}
export async function updateProfile() {}
export async function signInWithPopup() {throw new Error('Social sign-in is not configured in this local Java version. Use email login.');}
export async function sendPasswordResetEmail() {throw new Error('Email recovery is not configured in this local version.');}
