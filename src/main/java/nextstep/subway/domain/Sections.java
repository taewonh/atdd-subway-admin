package nextstep.subway.domain;

import static nextstep.subway.message.ErrorMessage.SECTION_ADD_DISTANCE_IS_BIG;
import static nextstep.subway.message.ErrorMessage.SECTION_IS_NO_SEARCH;
import static nextstep.subway.message.ErrorMessage.SECTION_STATION_IS_NO_SEARCH;
import static nextstep.subway.message.ErrorMessage.SECTION_STATION_NO_DELETE_RESON_ONE_SECTION;
import static nextstep.subway.message.ErrorMessage.SECTION_UP_STATION_AND_DOWN_STATION_EXIST;
import static nextstep.subway.message.ErrorMessage.SECTION_UP_STATION_AND_DOWN_STATION_NO_EXIST;
import static nextstep.subway.message.ErrorMessage.STATION_IS_NO_SEARCH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

@Embeddable
public class Sections {

    @OneToMany(cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "sectionId")
    private List<Section> sectionElement = new ArrayList<>();
    private static final int INIT_SELECTIONS_SIZE = 1;


    protected Sections() {

    }

    public Sections(Section newSection) {
        this.sectionElement.add(newSection);
    }


    public List<Section> getList() {
        sortedSections();
        return Collections.unmodifiableList(sectionElement);
    }

    public void addSection(Section newSection) {
        validExistUpStationAndDownStation(newSection); // 구간 존재
        validNoExistUpStationAndDownStation(newSection); // 상행선 하행선 존재하지 않음

        if (isBetweenAddSection(newSection)) {
            addBetweenSection(newSection);
            return;
        }
        this.sectionElement.add(newSection);
    }

    //상행종점
    public Station getLastUpStation() {
        List<Station> downStations = getDownStations()
                .collect(Collectors.toList());

        return getUpStations()
                .filter(stationsIsNotContains(downStations))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(STATION_IS_NO_SEARCH.toMessage()));
    }

    public Station getLastDownStation() {
        List<Station> upStations = getUpStations()
                .collect(Collectors.toList());

        return getDownStations()
                .filter(stationsIsNotContains(upStations))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(STATION_IS_NO_SEARCH.toMessage()));

    }

    public void deleteSectionStation(Station station) {
        validDeleteSectionStation(station);

        if (isLastUpStation(station)) {
            this.sectionElement.remove(findUpSection(station));
            return;
        }
        if (isLastDownStation(station)) {
            this.sectionElement.remove(findDownSection(station));
            return;
        }
        sortedSections();
    }

    private boolean isLastDownStation(Station station) {
        return station.equals(getLastDownStation());
    }

    private boolean isLastUpStation(Station station) {
        return station.equals(getLastUpStation());
    }

    private void validDeleteSectionStation(Station station) {
        if (this.sectionElement.size() == INIT_SELECTIONS_SIZE) {
            throw new IllegalStateException(SECTION_STATION_NO_DELETE_RESON_ONE_SECTION.toMessage());
        }

        if (!isContainStation(station)) {
            throw new IllegalStateException(SECTION_STATION_IS_NO_SEARCH.toMessage());
        }
    }

    private boolean isContainStation(Station station) {
        return this.sectionElement.stream()
                .anyMatch(section ->
                        section.getUpStation().equals(station) || section.getDownStation().equals(station));
    }


    private Section findUpSection(Station findStation) {
        return this.sectionElement.stream()
                .filter(section -> section.getUpStation().equals(findStation))
                .findFirst().get();
    }

    private Section findDownSection(Station findStation) {
        return this.sectionElement.stream()
                .filter(section -> section.getDownStation().equals(findStation))
                .findFirst().get();
    }

    private Predicate<Station> stationsIsNotContains(List<Station> searchStations) {
        return station -> !searchStations.contains(station);
    }


    private Stream<Station> getDownStations() {
        return this.sectionElement
                .stream()
                .map(Section::getDownStation);
    }

    private Stream<Station> getUpStations() {
        return this.sectionElement
                .stream()
                .map(Section::getUpStation);
    }

    private void addBetweenSection(Section newSection) {
        Section beforeSection = findAddSection(newSection);
        validDistance(newSection, beforeSection);

        beforeSection.minusDistance(newSection.getDistance());
        this.sectionElement.add(newSection);
    }


    private void sortedSections() {
        List<Section> list = new ArrayList<>();

        while (!sectionElement.isEmpty()) {
            Section lastUpSection = findUpSection(getLastUpStation());
            list.add(lastUpSection);
            sectionElement.remove(lastUpSection);
        }

        sectionElement = list;
    }


    private void validDistance(Section newSection, Section beforeSection) {
        if (!newSection.getDistance().isLess(beforeSection.getDistance())) {
            throw new IllegalStateException(SECTION_ADD_DISTANCE_IS_BIG.toMessage());
        }
    }

    private boolean isBetweenAddSection(Section newSection) {
        return isExistDownStation(newSection);
    }

    private Section findAddSection(Section newSection) {
        return sectionElement.stream()
                .filter((section -> section.getDownStation().equals(newSection.getDownStation())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(SECTION_IS_NO_SEARCH.toMessage()));
    }

    private void validExistUpStationAndDownStation(Section newSection) {
        if (isExistUpStation(newSection) && isExistDownStation(newSection)) {
            throw new IllegalArgumentException(SECTION_UP_STATION_AND_DOWN_STATION_EXIST.toMessage());
        }
    }

    private void validNoExistUpStationAndDownStation(Section newSection) {
        Set<Station> distinctSet = new HashSet<>();
        sectionElement.forEach((section -> {
            distinctSet.add(section.getDownStation());
            distinctSet.add(section.getUpStation());
        }));

        if (!distinctSet.contains(newSection.getUpStation()) && !distinctSet.contains(newSection.getDownStation())) {
            throw new IllegalStateException(SECTION_UP_STATION_AND_DOWN_STATION_NO_EXIST.toMessage());
        }
    }

    private boolean isExistDownStation(Section newSection) {
        return sectionElement.stream()
                .anyMatch(section -> section.getDownStation().equals(newSection.getDownStation()));
    }

    private boolean isExistUpStation(Section newSection) {
        return sectionElement.stream()
                .anyMatch(section -> section.getUpStation().equals(newSection.getUpStation()));
    }

}
