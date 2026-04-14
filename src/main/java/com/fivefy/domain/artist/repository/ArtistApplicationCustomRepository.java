package com.fivefy.domain.artist.repository;

import com.fivefy.domain.artist.entity.ArtistApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArtistApplicationCustomRepository {
    Page<ArtistApplication> searchArtistApplications(Pageable pageable);
}
