// Mock sessionStorage and localStorage for Node test environment
const createStorageMock = () => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => key in store ? store[key] : null,
    setItem: (key: string, value: string) => { store[key] = value; },
    removeItem: (key: string) => { delete store[key]; },
    clear: () => { store = {}; },
    get length() { return Object.keys(store).length; },
    key: (index: number) => Object.keys(store)[index] || null,
  };
};

Object.defineProperty(global, 'sessionStorage', { 
  value: createStorageMock(), 
  writable: true 
});

Object.defineProperty(global, 'localStorage', { 
  value: createStorageMock(), 
  writable: true 
});
