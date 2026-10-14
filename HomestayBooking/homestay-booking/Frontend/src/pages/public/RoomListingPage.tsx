import PublicLayout from '../../layouts/PublicLayout';
import { SearchResultsContent } from './SearchResultsPage';

/** SCR-07 — Room listing dùng cùng bộ lọc & layout với trang /search */
export default function RoomListingPage() {
  return (
    <PublicLayout>
      <SearchResultsContent variant="rooms" />
    </PublicLayout>
  );
}
