package postgres

import (
	"errors"
	"strings"

	"kitty-circle/internal/domain"

	"github.com/jackc/pgx/v5"
)

const listingCols = "id, seller_id, type, title, description, price_cents, currency, media_ids, created_at"

func scanListing(row pgx.Row) (domain.Listing, error) {
	var l domain.Listing
	var t string
	var mediaIDs []int64
	err := row.Scan(&l.ID, &l.SellerID, &t, &l.Title, &l.Description, &l.PriceCents, &l.Currency, &mediaIDs, &l.CreatedAt)
	if err != nil {
		return domain.Listing{}, err
	}
	l.Type = domain.ListingType(t)
	l.MediaIDs = mediaIDs
	return l, nil
}

func (s *Store) CreateListing(input domain.Listing) domain.Listing {
	ctx, cancel := bg()
	defer cancel()
	if input.Currency == "" {
		input.Currency = "CNY"
	}
	row := s.pool.QueryRow(ctx, `
		INSERT INTO listings (seller_id, type, title, description, price_cents, currency, media_ids)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
		RETURNING `+listingCols,
		input.SellerID, string(input.Type), input.Title, input.Description,
		input.PriceCents, input.Currency, nonNilInt64s(input.MediaIDs))
	l, err := scanListing(row)
	if err != nil {
		logErr("CreateListing", err)
		return domain.Listing{}
	}
	return l
}

func (s *Store) ListListings() []domain.Listing {
	return s.listListings(`SELECT `+listingCols+` FROM listings ORDER BY created_at DESC`, nil)
}

func (s *Store) ListListingsBySeller(sellerID int64) []domain.Listing {
	return s.listListings(`SELECT `+listingCols+` FROM listings WHERE seller_id=$1 ORDER BY created_at DESC`, []any{sellerID})
}

func (s *Store) GetListing(listingID int64) (domain.Listing, bool) {
	ctx, cancel := bg()
	defer cancel()
	row := s.pool.QueryRow(ctx, `SELECT `+listingCols+` FROM listings WHERE id=$1`, listingID)
	l, err := scanListing(row)
	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			logErr("GetListing", err)
		}
		return domain.Listing{}, false
	}
	return l, true
}

func (s *Store) UpdateListing(listing domain.Listing) bool {
	ctx, cancel := bg()
	defer cancel()
	tag, err := s.pool.Exec(ctx, `
		UPDATE listings SET type=$2, title=$3, description=$4, price_cents=$5, currency=$6, media_ids=$7
		WHERE id=$1`,
		listing.ID, string(listing.Type), listing.Title, listing.Description,
		listing.PriceCents, listing.Currency, nonNilInt64s(listing.MediaIDs))
	if err != nil {
		logErr("UpdateListing", err)
		return false
	}
	return tag.RowsAffected() > 0
}

func (s *Store) DeleteListing(listingID int64) bool {
	ctx, cancel := bg()
	defer cancel()
	tag, err := s.pool.Exec(ctx, `DELETE FROM listings WHERE id=$1`, listingID)
	if err != nil {
		logErr("DeleteListing", err)
		return false
	}
	return tag.RowsAffected() > 0
}

func (s *Store) SearchListings(keyword string) []domain.Listing {
	keyword = strings.TrimSpace(keyword)
	if keyword == "" {
		return nil
	}
	like := "%" + strings.ToLower(keyword) + "%"
	return s.listListings(`
		SELECT `+listingCols+` FROM listings
		WHERE LOWER(title) LIKE $1 OR LOWER(description) LIKE $1
		ORDER BY created_at DESC LIMIT 50`, []any{like})
}

func (s *Store) listListings(sql string, args []any) []domain.Listing {
	ctx, cancel := bg()
	defer cancel()
	rows, err := s.pool.Query(ctx, sql, args...)
	if err != nil {
		logErr("listListings", err)
		return nil
	}
	defer rows.Close()
	out := make([]domain.Listing, 0, 16)
	for rows.Next() {
		l, err := scanListing(rows)
		if err != nil {
			continue
		}
		out = append(out, l)
	}
	return out
}
