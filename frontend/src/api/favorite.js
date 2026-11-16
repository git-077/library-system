import request from '../utils/request'

export const addFavorite = bookId => request.post(`/favorites/${bookId}`)
export const removeFavorite = bookId => request.delete(`/favorites/${bookId}`)
export const getMyFavorites = () => request.get('/favorites/my')
