-- Optional, reviewed cutover import after 007. NOT part of automatic bootstrap.
-- Source tables and files remain intact. Safe to re-run: provenance keys are unique.
-- Old photos have not passed the new EXIF/scope pipeline, and old statuses do
-- not prove dual consent. Import to private drafts / legacy_review, not public
-- verified content or completed handovers. Owners review and republish via v2.
BEGIN;

INSERT INTO mobile_accounts(owner_id,record_key,state,data)
SELECT id,id::text,'active','{}'::jsonb FROM users
ON CONFLICT(record_key) WHERE record_key<>'' DO NOTHING;

INSERT INTO mobile_pets(owner_id,record_key,state,data,created_at,updated_at)
SELECT rescuer_id,'legacy-adoption-pet:'||id,'active',
 jsonb_build_object('name',name,'species',species,
  'breed',breed,'bio',description,'health',health,'adoption_available',false,
  'audience',jsonb_build_object('mode','private')),created_at,updated_at
FROM adoption_pets
ON CONFLICT(record_key) WHERE record_key<>'' DO NOTHING;

INSERT INTO mobile_adoptions(owner_id,participants,record_key,reference_id,state,data,created_at,updated_at)
SELECT a.rescuer_id,ARRAY[a.applicant_id],
 'legacy-application:'||a.id,p.id,'legacy_review',
 jsonb_build_object('pet_id',p.id,'applicant_id',a.applicant_id,'publisher_id',a.rescuer_id,
  'message',a.message,'trial_confirmations','[]'::jsonb,'handover_confirmations','[]'::jsonb,
  'note','历史状态待人工核对：'||a.status),a.created_at,a.updated_at
FROM adoption_applications a JOIN mobile_pets p ON p.record_key='legacy-adoption-pet:'||a.pet_id
ON CONFLICT(record_key) WHERE record_key<>'' DO NOTHING;

INSERT INTO mobile_entries(owner_id,record_key,state,data,created_at,updated_at)
SELECT author_id,'legacy-post:'||id,'draft',
 jsonb_build_object('title',title,'text',content,'category',category,'tags',to_jsonb(tags),
  'space','community','audience',jsonb_build_object('mode','private')),created_at,created_at
FROM posts
ON CONFLICT(record_key) WHERE record_key<>'' DO NOTHING;

INSERT INTO mobile_entry_comments(owner_id,record_key,reference_id,state,data,created_at,updated_at)
SELECT c.author_id,'legacy-comment:'||c.id,p.id,'published',jsonb_build_object('text',c.content),c.created_at,c.created_at
FROM comments c JOIN mobile_entries p ON p.record_key='legacy-post:'||c.post_id
ON CONFLICT(record_key) WHERE record_key<>'' DO NOTHING;

INSERT INTO mobile_entry_reactions(owner_id,record_key,reference_id,state,data,created_at,updated_at)
SELECT l.user_id,p.id||':'||l.user_id,p.id,'active','{"liked":true}'::jsonb,l.created_at,l.created_at
FROM post_likes l JOIN mobile_entries p ON p.record_key='legacy-post:'||l.post_id
ON CONFLICT(record_key) WHERE record_key<>'' DO NOTHING;

-- Existing follow edges are NOT mutual friendships. Orders, legacy payment
-- outcomes, identity verification claims and public image URLs are NOT copied.
-- Keep old conversations read-only under v1 until users accept a new v2 request;
-- no inferred consent or silent expansion of image-sharing permissions.
COMMIT;
